package com.ytu;

import static org.junit.Assert.assertTrue;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.beans.ImmutableBean;
import net.sf.cglib.proxy.*;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    /**
     * EnHancer不能对final类和final方法进行拦截，因为他们不能被继承，static静态方法和静态类也不能被代理
     */
    @Test
    public void testHelloWorld()
    {
        Enhancer enHancer = new Enhancer();
        enHancer.setSuperclass(SampleClass.class);
        enHancer.setCallback(new MethodInterceptor() {
            /**
             *
             * @param o 代理对象
             * @param method 被代理类的方法
             * @param objects 被调用方法的参数
             * @param methodProxy 代理的方法
             * @return
             * @throws Throwable
             */
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                System.out.println("before method run...");
                Object result = methodProxy.invokeSuper(o,objects);
                System.out.println("after method run...");
                return result;
            }
        });
        //创建子类的代理对象
        SampleClass sample = (SampleClass) enHancer.create();
        sample.test();
    }

    /**
     * 使用fixeValue来进行回调
     */
    @Test
    public void testFixedValue()
    {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(SampleClass.class);
        enhancer.setCallback(new FixedValue() {
            @Override
            public Object loadObject() throws Exception {
                return "hello cglib";
            }
        });
        SampleClass sampleClass = (SampleClass) enhancer.create();
        sampleClass.test();
        sampleClass.toString();
        //这个方法是final,所以他不会被拦截
        sampleClass.getClass();
        //返回类型不匹配，报错
       // sampleClass.hashCode();

    }

    @Test
    public void testInvocationHandler()
    {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(SampleClass.class);
        enhancer.setCallback(new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                if(method.getDeclaringClass()!=Object.class&&method.getReturnType()==String.class)
                {
                    return "hello cglib";
                }else{
                    throw new RuntimeException("Do not know what to do");
                }
            }
        });
        SampleClass proxy = (SampleClass) enhancer.create();
        Assert.assertEquals("hello cglib", proxy.test1());
        System.out.println(1);
        Assert.assertNotEquals("Hello cglib", proxy.toString());
    }
    /**
     * 设置一个统一的回调对特定的对一个类里面特定的某些方法进行拦截，所以他是一个统一回调函数
     */
    @Test
    public void testCallbackFilter()
    {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(SampleClass.class);
        CallbackHelper callbackHelper = new CallbackHelper(SampleClass.class,new Class[0]) {
            @Override
            protected Object getCallback(Method method) {
                if(method.getDeclaringClass() != Object.class && method.getReturnType() == String.class){
                    return new FixedValue() {
                        @Override
                        public Object loadObject() throws Exception {
                            return "hello cglib";
                        }
                    };
                }else{
                    //返回NoOp.INSTANCE是指放行这个方法，不进行拦截
                    return NoOp.INSTANCE;
                }
            }
        };
        enhancer.setCallbackFilter(callbackHelper);
        enhancer.setCallbacks(callbackHelper.getCallbacks());
        SampleClass proxy = (SampleClass) enhancer.create();
        System.out.println(proxy.test1());
        System.out.println(proxy.toString());
        System.out.println(proxy.hashCode());
    }
    /**
     * cglib支持不可变的bean(ImmutableBean)
     */
    @Test(expected = IllegalStateException.class)
    public void testImmutableBean() throws Exception
    {
        SampleBean bean = new SampleBean();
        bean.setValue("hello world");
        //创建不可变对象
        SampleBean immctableBean = (SampleBean) ImmutableBean.create(bean);
        Assert.assertEquals("hello world",immctableBean.getValue());
        //原对象可以修改底层对象
        bean.setValue("Hello world, again");
        Assert.assertEquals("Hello world, again",immctableBean.getValue());
        //不能修改底层对象的值,会报IllegalStateException异常
        immctableBean.setValue("hello");
        System.out.println(immctableBean.getValue());
    }
    /**
     * 使用cglib动态的生成一个bean
     */
    @Test
    public void testBeanGenerator() throws Exception
    {
        BeanGenerator beanGenerator = new BeanGenerator();
        //添加属性值
        beanGenerator.addProperty("value",String.class);
        Object myBean = beanGenerator.create();
        Method setter = myBean.getClass().getMethod("setValue",String.class);
        setter.invoke(myBean,"hello cglib");
        Method getter = myBean.getClass().getMethod("getValue");
        System.out.println(getter.invoke(myBean));
    }

}
