package org.simpleframework.aop;

import org.simpleframework.aop.annotation.Aspect;
import org.simpleframework.aop.annotation.Order;
import org.simpleframework.aop.aspect.AspectInfo;
import org.simpleframework.aop.aspect.DefaultAspect;
import org.simpleframework.core.BeanContainer;
import org.simpleframework.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AspectWeaver {
    private BeanContainer beanContainer;

    public AspectWeaver() {
        this.beanContainer = BeanContainer.getInstance();
    }

    public void doAop() {
        //1.获取所有的切面
        Set<Class<?>> aspectSet = beanContainer.getClassesByAnnotation(Aspect.class);
        if (ValidationUtil.isEmpty(aspectSet)) {return;}
        //2.拼装所有的切面信息
        List<AspectInfo> aspectInfoList = packAspectInfoList(aspectSet);
        //3.遍历容器的类
        Set<Class<?>> classSet = beanContainer.getClasses();
        for (Class clazz : classSet) {
            //排除自己本身
            if (clazz.isAnnotationPresent(Aspect.class))
                continue;
            //4.筛选符合条件的aspect
            List<AspectInfo> matchedAspectList = collectMatchedAspectListForClass(aspectInfoList, clazz);
            //5.进行aspect织入
            wrapIfNecessary(matchedAspectList, clazz);
        }


    }

    private void wrapIfNecessary(List<AspectInfo> roughMatchedAspectList, Class<?> targetClass) {
        if(ValidationUtil.isEmpty(roughMatchedAspectList)){return;}
        //创建动态代理对象
        AspectListExecutor aspectListExecutor = new AspectListExecutor(targetClass, roughMatchedAspectList);
        Object proxyBean = ProxyCreator.createProxy(targetClass, aspectListExecutor);
        beanContainer.addBean(targetClass, proxyBean);
    }

    private List<AspectInfo> collectMatchedAspectListForClass(List<AspectInfo> aspectInfoList, Class clazz) {
        List<AspectInfo> matchedAspectInfoList = new ArrayList<>();
        for (AspectInfo aspectInfo : aspectInfoList) {
            if (aspectInfo.getPointcutLocator().roughMatches(clazz)) {
                matchedAspectInfoList.add(aspectInfo);
            }
        }
        return matchedAspectInfoList;
    }

    private List<AspectInfo> packAspectInfoList(Set<Class<?>> aspectSet) {
        List<AspectInfo> aspectInfoList = new ArrayList<>();
        for (Class<?> clazz : aspectSet) {
            if (verifyAspect(clazz)){
                Order order = clazz.getAnnotation(Order.class);
                Aspect aspect = clazz.getAnnotation(Aspect.class);
                DefaultAspect defaultAspect = (DefaultAspect) beanContainer.getBean(clazz);
                //初始化表达式定位器
                PointcutLocator pointcutLocator = new PointcutLocator(aspect.pointcut());
                AspectInfo aspectInfo = new AspectInfo(order.value(), defaultAspect, pointcutLocator);
            } else {
                throw new RuntimeException("@Aspect and @Order must be added to the Aspect class, and Aspect class must extend from DefaultAspect");
            }
        }
        return aspectInfoList;
    }

    private boolean verifyAspect(Class<?> clazz) {
        return clazz.isAnnotationPresent(Aspect.class) &&
                clazz.isAnnotationPresent(Order.class) &&
                DefaultAspect.class.isAssignableFrom(clazz);
    }
}
