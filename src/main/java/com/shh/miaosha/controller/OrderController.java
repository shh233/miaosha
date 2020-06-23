package com.shh.miaosha.controller;

import com.alibaba.druid.util.StringUtils;
import com.google.common.util.concurrent.RateLimiter;
import com.shh.miaosha.Response.CommonReturnType;
import com.shh.miaosha.error.EmBusinessError;
import com.shh.miaosha.error.ErrorException;
import com.shh.miaosha.mq.TransactionMqProducer;
import com.shh.miaosha.service.ItemService;
import com.shh.miaosha.service.OrderService;
import com.shh.miaosha.service.PromoService;
import com.shh.miaosha.service.model.OrderModel;
import com.shh.miaosha.service.model.PromoModel;
import com.shh.miaosha.service.model.UserModel;
import com.shh.miaosha.util.CodeUtil;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.util.Map;
import java.util.concurrent.*;

@RestController
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true" , allowedHeaders = "*")
public class OrderController extends BaseController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TransactionMqProducer transactionMqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter createOrderRateLimiter;

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);
        createOrderRateLimiter = RateLimiter.create(300);
    }

    @RequestMapping(value = "/generateverifycode")
    public void generateVerifyCode(HttpServletResponse response,
            @RequestParam(name = "token") String token) throws Exception {

        if(StringUtils.isEmpty(token)){
            throw new ErrorException(EmBusinessError.USER_NOT_LOGIN, "token为空，用户还没登陆，不能生成验证码");
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new ErrorException(EmBusinessError.USER_NOT_LOGIN, "userModel为空，用户还没登陆，不能生成验证码");
        }


        Map<String, Object> codeMap = CodeUtil.generateCodeAndPic();

        redisTemplate.opsForValue().set("verify_code_"+userModel.getId(), codeMap.get("code"));
        redisTemplate.expire("verify_code_"+userModel.getId(), 10, TimeUnit.MINUTES);

        //将BufferedImage，传到前端
        ImageIO.write((RenderedImage) codeMap.get("codePic"), "jpeg", response.getOutputStream());

    }


            @RequestMapping(value = "/generatepromotoken" , method = {RequestMethod.POST} , consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType generateToken(@RequestParam(name = "promoId") Integer promoId,
                                          @RequestParam(name = "itemId") Integer itemId,
                                          @RequestParam(name = "token") String token,
                                          @RequestParam(name = "verifyCode") String verifyCode ) throws Exception{
        //根据token获取用户信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new ErrorException(EmBusinessError.USER_NOT_LOGIN, "用户还没登陆");
        }

        //验证验证码的有效性
        String inRedisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_"+userModel.getId());

        if(StringUtils.isEmpty(inRedisVerifyCode)){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR,"redis中的验证码为空");
        }

        if(!StringUtils.equalsIgnoreCase(inRedisVerifyCode, verifyCode)){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR,"验证码错误");
        }

        //生成秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if(promoToken == null){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR,"秒杀令牌不存在");
        }


        //将promoToken返回给前端
        return CommonReturnType.create(promoToken);
    }

    @RequestMapping(value = "/createorder" , method = {RequestMethod.POST} , consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "token") String token,
                                        @RequestParam(name = "promoId", required = false) String promoId,
                                        @RequestParam(name = "promoToken", required = false) String promoToken) throws Exception {
        //检查用户是否登录
       /* Boolean isLogin = (Boolean) this.httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(isLogin==null || !isLogin){
            throw new ErrorException(EmBusinessError.USER_NOT_LOGIN);
        }
        UserModel userModel = (UserModel)this.httpServletRequest.getSession().getAttribute("LOGIN_USER");
*/


       //令牌桶
        if(!createOrderRateLimiter.tryAcquire()){
            throw new ErrorException(EmBusinessError.RATELIMITER);
        }


       //根据token获取
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new ErrorException(EmBusinessError.USER_NOT_LOGIN);
        }

        //校验秒杀令牌
        if(promoToken != null){

            String redisPromoToken = (String) redisTemplate.opsForValue().get("promo_"+promoId+"_item_"+itemId+"_user_"+userModel.getId());
            if(!StringUtils.equals(promoToken, redisPromoToken)){
                throw  new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "秒杀令牌不匹配");
            }
        }


        //同步调用线程池submit方法
        //拥塞窗口为20的等待队列，用来队列泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                //初始化流水状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                //System.out.println(userModel);
                //OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, amount);
                boolean mqResult = transactionMqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, amount, stockLogId);
                if(!mqResult){
                    throw new ErrorException(EmBusinessError.UNKNOW_ERROE, "下单失败");
                }

                return null;
            }
        });
        future.get();


        return CommonReturnType.create(null);
    }


}
