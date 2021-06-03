package org.simpleframework.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.simpleframework.core.annotation.Component;
import org.simpleframework.core.annotation.Controller;
import org.simpleframework.core.annotation.Repository;
import org.simpleframework.core.annotation.Service;
import org.simpleframework.util.ClassUtil;
import org.simpleframework.util.ValidationUtil;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanContainer {

    private final Map<Class<?>, Object> beanMap = new ConcurrentHashMap<>();

    private static final List<Class<? extends Annotation>> BEAN_ANNOTATION  = Arrays.asList(Component.class, Controller.class, Service.class, Repository.class);

    public static BeanContainer getInstance() {
        return ContainerHolder.HOLDER.instance;
    }

    public Object getBean(Class<?> clazz) {
        return beanMap.get(clazz);
    }

    public Set<Class<?>> getClassesBySuper(Class<?> interfaceOrClass) {
        //1.获取beanMap的所有class对象
        Set<Class<?>> keySet = getClasses();
        if(ValidationUtil.isEmpty(keySet)){
            log.warn("nothing in beanMap");
            return null;
        }
        //2.判断keySet里的元素是否是传入的接口或者类的子类，如果是，就将其添加到classSet里
        Set<Class<?>> classSet = new HashSet<>();
        for (Class<?> clazz : keySet) {
            if (interfaceOrClass.isAssignableFrom(clazz) && !clazz.equals(interfaceOrClass)) {
                classSet.add(clazz);
            }
        }
        return classSet.size() >  0 ? classSet: null;
    }

    enum ContainerHolder {
        HOLDER;
        private BeanContainer instance;
        ContainerHolder() {
            instance = new BeanContainer();
        }
    }


    /**
     * 容器是否已经加载过bean
     */
    private boolean loaded = false;

    /**
     * 是否已加载过Bean
     *
     * @return 是否已加载
     */
    public boolean isLoaded() {
        return loaded;
    }

    public synchronized void loadBeans(String packageName) {
        if (loaded){
            log.warn("BeanContainer has bean loaded");
            return;
        }
        Set<Class<?>> classSet = ClassUtil.extractPackageClass(packageName);
        for (Class<?> clazz : classSet) {
            for (Class<? extends Annotation> annotation : BEAN_ANNOTATION) {
                //如果类上面标记了定义的注解
                if (clazz.isAnnotationPresent(annotation)) {
                    //将目标类本身作为键，目标类的实例作为值，放入到beanMap中
                    beanMap.put(clazz, ClassUtil.newInstance(clazz, true));
                }
            }
        }
        loaded =true;
    }

    public Set<Class<?>> getClasses(){
        return beanMap.keySet();
    }

}
