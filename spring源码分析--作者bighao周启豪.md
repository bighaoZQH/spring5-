#Spring IOC源码分析之javaConfig版 -- 作者bighao周启豪

**为了看spring的源码，今年自己也是花了3个月的时间，从0开始看的，一开始都不知道从哪下手，所以也是从网上找了很多的帖子一步一步来，强行看多了就慢慢开始熟悉了起来，也有了自己的理解。过程也很痛苦，很多复杂的方法只能自己结合网上的文档先尝试强行理解分析后再加上反复debug调试，再反复分析理解才能真正的懂。**



基于Spring5.0.X版本分析

spring的配置方式有三种，一种是xml，一种是javaConfig来配置，还有就是注解

这里的源码分析是基于javaConfig + 注解的方式配置来分析的，没有xml的



我们就传入一个配置类来进行分析，这里先不要去纠结我的配置类里的内容是什么

## 一.入口

AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
这个构造方法需要传入一个javaConfig,即注解了的配置类,然后会把这个被注解了的配置类通过注解读取器读取后进而解析



接下来我们点进这个构造方法看到:

1.由于他有父类，首先调用了父类的构造方法

2.调用自己的构造方法, 这个构造方法里spring注册了6个他自己内部的bean，很重要

3.读取bean,将bean的定义放入工厂类里的map中

4.初始化spring的环境

接下来就挨个分析上面的步骤

```java
public AnnotationConfigApplicationContext(Class<?>... annotatedClasses) {
	// 先调用父类构造
	// 在自己的构造方法中初始一个读取器和扫描器
	// 以实现将相应配置类中的Bean自动注册到容器中
	this();
	// 读取bean,将bean的定义放入工厂类里的map中
	register(annotatedClasses);
	// 初始化spring的环境
	refresh();
}
```



##二.调用父类的构造方法 实际上就是创建bean工厂

创建了一个DefaultListableBeanFactory对象，就是我们所说的bean工厂，这里仅仅是创建了一个空工厂

```java
public GenericApplicationContext() {
	this.beanFactory = new DefaultListableBeanFactory();
}
```



##三. this(); 调用本类无参构造方法

```java
/**
	 * Create a new AnnotationConfigApplicationContext that needs to be populated
	 * through {@link #register} calls and then manually {@linkplain #refresh refreshed}.
	 */
public AnnotationConfigApplicationContext() {
    /**
	  * 会先调用父类的构造方法
	  * 创建一个读取注解的Bean定义读取器
	  */
    this.reader = new AnnotatedBeanDefinitionReader(this);

    // 可以用来扫描包或者类，继而转换成bd
    // 但是实际上我们扫描包工作不是scanner这个对象来完成的
    // 是spring自己new的一个ClassPathBeanDefinitionScanner
    // 这里的scanner仅仅是为了程序员能够在外部调用AnnotationConfigApplicationContext对象的scan方法
    this.scanner = new ClassPathBeanDefinitionScanner(this);
}
```

###3.1 构建并对外提供bean定义的读取器和扫描器

这个构造方法里主要就是初始化了一个reader对象和scanner对象，并对外提供，这里的scanner在spring内部没有用到，这里之所以new了一个scanner对象，就是对外提供一种扩展，可以让我们在外部进行调用AnnotationConfigApplicationContext对象的scan方法，那么我们这里暂时先不分析new ClassPathBeanDefinitionScanner(this);的过程。



###3.2 构建bean定义的读取器的过程

下面我分析下初始化bean定义读取器的过程

这个过程很重要，Spring在这个过程中，向容器中添加了6个Spring的内部bean

```java
this.reader = new AnnotatedBeanDefinitionReader(this);
```

这里大家要注意到，这里传入的对象是this，也就是AnnotationConfigApplicationContext这个类，然后我们看到AnnotatedBeanDefinitionReader中的这个构造方法要求传入的是一个BeanDefinitionRegistry对象，这里由此

说明了AnnotationConfigApplicationContext和BeanDefinitionRegistry肯定是同宗同源的

```
public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		this(registry, getOrCreateEnvironment(registry));
}
```

我们来看下关系类图

![1584455642287](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584455642287.png)

那么BeanDefinitionRegistry其实就是一个BeanDefinition的注册器，那么什么是BeanDefinition? 下面我再来介绍，下面我接着分析其构造的逻辑

最终其调用到了这一个方法

```java
public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(
			BeanDefinitionRegistry registry, @Nullable Object source) {

    DefaultListableBeanFactory beanFactory = unwrapDefaultListableBeanFactory(registry);
    if (beanFactory != null) {
        if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
            // AnnotationAwareOrderComparator主要能解析@Order注解和@Priority
            beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
        }
        if (!(beanFactory.getAutowireCandidateResolver() instanceof ContextAnnotationAutowireCandidateResolver)) {
            // ContextAnnotationAutowireCandidateResolver提供处理延迟加载的功能
            beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
        }
    }

    Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);

    // BeanDefinitio的注册，这里很重要，需要理解注册每个bean的类型
    if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        // 需要注意的是ConfigurationClassPostProcessor的类型是BeanDefinitionRegistryPostProcessor
        // 而 BeanDefinitionRegistryPostProcessor 最终实现BeanFactoryPostProcessor这个接口
        RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
        // RootBeanDefinition是描述spring内部的bean的
        def.setSource(source);
        // 通过registerPostProcessor将RootBeanDefinition注册到工厂中的beanMap中
        // 实际上最终put到map中的方法 和 我们自己写的bean被put的方法是同一个
        beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        // AutowiredAnnotationBeanPostProcessor 实现了 MergedBeanDefinitionPostProcessor
        // MergedBeanDefinitionPostProcessor 最终实现了 BeanPostProcessor
        RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(RequiredAnnotationBeanPostProcessor.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    // Check for JSR-250 support, and if present add the CommonAnnotationBeanPostProcessor.
    if (jsr250Present && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    // Check for JPA support, and if present add the PersistenceAnnotationBeanPostProcessor.
    if (jpaPresent && !registry.containsBeanDefinition(PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition();
        try {
            def.setBeanClass(ClassUtils.forName(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME,
                                                AnnotationConfigUtils.class.getClassLoader()));
        }
        catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                "Cannot load optional framework class: " + PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, ex);
        }
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(EVENT_LISTENER_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(EventListenerMethodProcessor.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(EVENT_LISTENER_FACTORY_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(DefaultEventListenerFactory.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_FACTORY_BEAN_NAME));
    }

    return beanDefs;
}
```

作用:

1. 给beanFactory添加了一个排序器AnnotationAwareOrderComparator,主要能解析@Order注解和@Priority
2. .给beanFactory添加了一个ContextAnnotationAutowireCandidateResolver，用于提供处理延迟加载的功能
3. 就是将spring容器初始化时的6个内部bean包装成RootBeanDefinition再放入到ioc容器中，由此我们要知道，Spring在初始化这个reader对象的时候，实际上不仅仅创建了一个注册器这么简单，还顺带把Spring里的6个内部bean也注册到了IOC容器当中，spring之所以要在这里注册这6个类就是因为在spring初始化环境中会用到这些类，至于这6个类是怎么被注册到容器中的还有这6个类的作用，我后面再分析。



###3.3 BeanDefinition是什么？

BeanDefinition是一个接口，翻译过来是Bean的定义，什么是Bean的定义? 就是怎么来描述这个Bean。

首先我们要知道，当我们把一个类比交给Spring的IOC去管理以后，我们的这个类就不再单纯的是我们以前说的一个java类对象了，或者说对于jvm和我们程序员来说，它确实是一个java类，但是对于Spring来说，你也是一个类，但不仅仅是一个java类 ，还得是我Spring管理的类，我Spring可是占有欲很强的，我要在你这个对象上打上更多的标记。

什么意思? 我们先想想，java是一个面向对象的语言，什么是面向对象? 就是一切皆对象，那么我写的一个java类也是一个对象，那我怎么来定义这个类，或者说需要哪些内容来定义?  比如，这个类叫什么，包路径是什么，类加载器是哪个，有什么方法，方法参数，返回值类型，方法访问修饰符等等，那么java就把这么一些个东西抽象出来一个Class类，方法相关的抽象成一个Method类。Spring的BeanDefinition同理，你想想我们Spring的bean的一些特性, 比如作用域，是否懒加载，依赖关系，bean工厂是哪个等等，这些就是bean的定义的内容，那么Spring就需要把这些东西抽象出来，这就是BeanDefinition，由于要对其进行分类，因此这是个接口，再由其子类去做具体的实现。





首先我们要知道，当我们把一个类比交给Spring的IOC去管理以后，Spring要怎么来实例化bean?







## 四.register(annotatedClasses); 读取bean,将bean的定义放入工厂类里的map中 

### 4.1 解析bean，将bean封装成AnnotatedGenericBeanDefinition对象

register(Class<?>... annotatedClasses)该方法的作用是:

注册单个bean给容器比如有新加的类可以用这个方法但是注册注册之后需要手动调用refresh() 去触发容器解析注解 

它可以注册一个配置类，它也可以单独注册一个或多个bean，通过下面的代码看到他会循环去处理传入的多个Class对象，因此在具体的注册逻辑里他之所以会解析很多内容，就是因为这个方法不仅仅可以注册配置类，还可以注册bean。当然其实我们的配置类不就是一个bean嘛。

```java
public void register(Class<?>... annotatedClasses) {
    for (Class<?> annotatedClass : annotatedClasses) {
        registerBean(annotatedClass);
    }
}
```

那么最终调用到的方法是doRegisterBean()这个方法，在Spring当中，真正去执行逻辑的都是以do为开头的方法

```java
// Bean定义读取器向容器注册注解Bean定义类
<T> void doRegisterBean(Class<T> annotatedClass, @Nullable Supplier<T> instanceSupplier, @Nullable String name,@Nullable Class<? extends Annotation>[] qualifiers, BeanDefinitionCustomizer... definitionCustomizers) {
		/**
		 * 根据指定的bean创建一个AnnotatedGenericBeanDefinition
		 * 这个AnnotatedGenericBeanDefinition可以理解为一个数据结构
		 * AnnotatedGenericBeanDefinition包含了类的其他信息,比如一些元信息scope，lazy等等
		 * 为什么这里创建的是一个被注解的BeanDefinition呢? 因为spring要求传的就是annotatedClass
		 */
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(annotatedClass);

		// 通过@Conditional装配条件判断是否需要跳过注册
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}

		// 设置回调 @param instanceSupplier 用于创建bean实例的回调
		abd.setInstanceSupplier(instanceSupplier);
		// 第一步，解析注解Bean定义的作用域 默认singleton 若@Scope("prototype")，则Bean为原型类型；
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		// 把类的作用域添加到数据结构结构中
		abd.setScope(scopeMetadata.getScopeName());
		// 生成类的名字通过beanNameGenerator
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));

		/**
		 * 第二步，处理类当中的通用注解，分析源码可以知道他主要处理 Lazy DependsOn Primary Role等等注解
		 * 处理完成之后processCommonDefinitionAnnotations中依然是把他添加到数据结构当中
		 */
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
		/**
		 * 如果在向容器注册注解Bean定义时，使用了额外的限定符注解 则解析限定符注解
		 * 主要是配置的关于autowiring自动依赖注入装配的限定条件，即@Qualifier注解
		 * 关于Qualifier和Primary前面的课当中讲过，主要涉及到spring的自动装配
		 * 这里需要注意的
		 * byName和qualifiers这个变量是Annotation类型的数组，里面存不仅仅是Qualifier注解
		 * 理论上里面里面存的是一切注解，所以可以看到下面的代码spring去循环了这个数组
		 * 然后依次判断了注解当中是否包含了Primary，是否包含了Lazyd
		 */
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				// 如果配置了@Primary注解，设置该Bean为autowiring自动依赖注入装配时的首选
				if (Primary.class == qualifier) {
					abd.setPrimary(true);
				}
				// 如果配置了@Lazy注解，则设置该Bean为非延迟初始化，如果没有配置，则该Bean为【预】实例化
				else if (Lazy.class == qualifier) {
					abd.setLazyInit(true);
				}
				else {
					// 如果使用了除@Primary和@Lazy以外的其他注解，
					// 则为该Bean添加一个autowiring自动依赖注入装配限定符，
					// 该Bean在进autowiring自动依赖注入装配时，根据名称装配限定符指定的Bean
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}
		// 读取自定义注解 不重要
		for (BeanDefinitionCustomizer customizer : definitionCustomizers) {
			customizer.customize(abd);
		}

		// 这个BeanDefinitionHolder也是一个数据结构 创建一个指定Bean名称的Bean定义对象，封装注解Bean定义类数据
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		/**
		 * 第三步，根据注解Bean定义类中配置的作用域，创建相应的代理对象
		 * ScopedProxyMode 这个知识点比较复杂，需要结合web去理解
		 */
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		/**
		 * 第四步，向IOC容器注册注解Bean类定义对象
		 * 把上述的这个数据结构注册给registry
		 * registy就是AnnotatonConfigApplicationContext
		 * AnnotatonConfigApplicationContext在初始化的时候通过调用父类的构造方法
		 * 实例化了一个DefaultListableBeanFactory
		 * registerBeanDefinition里面就是把definitionHolder这个数据结构包含的信息注册到
		 * DefaultListableBeanFactory这个工厂
		 */
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}
```

该方法的逻辑是:

1.将我们传入的Class对象先预封装成一个AnnotatedGenericBeanDefinition对象，设置了beanClass和注解信息

2.通过@Conditional装配条件判断是否需要跳过注册

3.解析注解Bean定义的作用域 默认singleton 若@Scope("prototype")，则Bean为原型类型；

4.处理类当中的通用注解，分析源码可以知道他主要处理 Lazy DependsOn Primary Role等等注解，处理完成之后processCommonDefinitionAnnotations中依然是把他添加到数据结构(就是预封装的对象)当中

5.处理额外限定符，注意这个地方是spring自己在内部调用的时候才会有用，因为我们自己传入的时候可以看到我们的方法调用链传入的永远是null

6.读取自定义注解，这个没啥用

7.将beanName和封装好的AnnotatedGenericBeanDefinition对象再封装成一个BeanDefinitionHolder对象，

这个BeanDefinitionHolder其实主要是在当你是xml来配置的时候才有用的，因此这一步不要去纠结，因为如果你的spring是基于了xml来配置的也是要调了register()这个方法

8.根据注解Bean定义类中配置的作用域，创建相应的代理对象，ScopedProxyMode 这个知识点比较复杂，需要结合web去理解，注意这个代理模式不是指的AOP的代理

9.最后向IOC容器注册Bean类定义对象



总结一下就是:

1.将bean封装成bean定义类对象

2.解析作用域和通用注解

3.根据bean的作用域创建代理模式

4.向IOC容器注册Bean类定义对象



======================================我是分隔符(*^__^*) ===================================



###4.2接来下分析向IOC容器注册注解Bean类定义对象

调用到了下面的方法

```java
BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
```

```java
// 将解析的BeanDefinitionHold注册到容器中
public static void registerBeanDefinition(
    BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
    throws BeanDefinitionStoreException {

    // Register bean definition under primary name.
    // 获取解析的BeanDefinition的名称
    String beanName = definitionHolder.getBeanName();
    // 其实这边传入的还是一个definitionHolder，只不过拆开传入了
    registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

    // Register aliases for bean name, if any.
    // 如果解析的BeanDefinition有别名，向容器为其注册别名，不重要
    String[] aliases = definitionHolder.getAliases();
    if (aliases != null) {
        for (String alias : aliases) {
            registry.registerAlias(beanName, alias);
        }
    }
}
```

实际上可以看到这里也不重要，就是调用了另一个方法，然后向容器注册别名

接下来是要分析下面的代码

```java
registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());
```

调用到了DefaultListableBeanFactory类中, DefaultListableBeanFactory就是我们说的Bean工厂，这里先不要去纠结，后面会说到的

```java
// 向IOC容器册解析的BeanDefiniton
@Override
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
    throws BeanDefinitionStoreException {
    // 校验解析的BeanDefiniton
    Assert.hasText(beanName, "Bean name must not be empty");
    Assert.notNull(beanDefinition, "BeanDefinition must not be null");

    if (beanDefinition instanceof AbstractBeanDefinition) {
        try {
            /**
				 * 注册前的最后一次校验，这里的校验不同于XML文件校验，
				 * 主要是对于AbstractBeanDefinition属性中的methodOverrides校验，
				 * 校验methodOverrides是否与工厂方法并存或者methodOverrides对应的方法根本不存在
				 */
            ((AbstractBeanDefinition) beanDefinition).validate();
        }
        catch (BeanDefinitionValidationException ex) {
            throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                                                   "Validation of bean definition failed", ex);
        }
    }

    BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
    if (existingDefinition != null) {
        // 如果对应的BeanName已经注册且在配置中配置了bean不允许被覆盖，则抛出异常
        if (!isAllowBeanDefinitionOverriding()) {
            throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                                                   "Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
                                                   "': There is already [" + existingDefinition + "] bound.");
        }
        else if (existingDefinition.getRole() < beanDefinition.getRole()) {
            // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
            if (logger.isWarnEnabled()) {
                logger.warn("Overriding user-defined bean definition for bean '" + beanName +
                            "' with a framework-generated bean definition: replacing [" +
                            existingDefinition + "] with [" + beanDefinition + "]");
            }
        }
        else if (!beanDefinition.equals(existingDefinition)) {
            if (logger.isInfoEnabled()) {
                logger.info("Overriding bean definition for bean '" + beanName +
                            "' with a different definition: replacing [" + existingDefinition +
                            "] with [" + beanDefinition + "]");
            }
        }
        else {
            if (logger.isDebugEnabled()) {
                logger.debug("Overriding bean definition for bean '" + beanName +
                             "' with an equivalent definition: replacing [" + existingDefinition +
                             "] with [" + beanDefinition + "]");
            }
        }
        // 注册BeanDefinition
        this.beanDefinitionMap.put(beanName, beanDefinition);
    }
    else {
        if (hasBeanCreationStarted()) {
            // Cannot modify startup-time collection elements anymore (for stable iteration)
            // 注册的过程中需要线程同步，以保证数据的一致性
            // 因为beanDefinition是全局变量，这里肯定会存在并发访问的情况
            synchronized (this.beanDefinitionMap) {
                this.beanDefinitionMap.put(beanName, beanDefinition);
                List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
                updatedDefinitions.addAll(this.beanDefinitionNames);
                updatedDefinitions.add(beanName);
                this.beanDefinitionNames = updatedDefinitions;
                if (this.manualSingletonNames.contains(beanName)) {
                    Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
                    updatedSingletons.remove(beanName);
                    this.manualSingletonNames = updatedSingletons;
                }
            }
        }
        else {
            // Still in startup registration phase
            /**
			  * 想象一下DefaultListableBeanFactory是工厂，
			  * 而bean的描述都存放在这个工厂里的beanDefinitionMap中，
			  * 我们能得到什么bean就是看beanDefinitionMap有哪些bean的描述
			 */
            this.beanDefinitionMap.put(beanName, beanDefinition);
            this.beanDefinitionNames.add(beanName);
            this.manualSingletonNames.remove(beanName);
        }
        this.frozenBeanDefinitionNames = null;
    }

    // 检查是否有同名的BeanDefinition已经在IOC容器中注册
    if (existingDefinition != null || containsSingleton(beanName)) {
        // 重置所有已经注册过的BeanDefinition的缓存
        resetBeanDefinition(beanName);
    }
}
```

其实这里的代码虽然很多，但大多数都是在注册之前去校验,最后呢可以看到

this.beanDefinitionMap.put(beanName, beanDefinition);

就是说解析出来的beanDefinition最终都被放到了DefaultListableBeanFactory中的一个map中去

```java
/**
 * 存储注册信息的BeanDefinition
 */
BeanDefinitionprivate final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
```

但是注意了！！！ 如果断点打在这你会发现为什么注册的bean不是我们传入的配置类，这就是AnnotationConfigApplicationContext 他自己调用了无参构造方法时干的事情，Spring内部去创建了6个内部bean注册到该容器当中，也就是说实际上我的断点前6次到这里的时候，实际上并不是我上面分析的调用链调用过来的，而是在另一个无参构造方法的调用链里调用过来的，这里先要去纠结那6个bean，后面我会分析到

因此当我的断点加上判断条件后再来看beanDefinitionMap里的数据，可以看到map里已经被注册了6个spring的内部bean了

![1584434158095](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584434158095.png)



至此register(annotatedClasses);的分析就大致完成了



##五.初始化Spring环境refresh();

refresh();里一共调用了12个方法

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // Prepare this context for refreshing.
        // 1.准备工作包括设置启动时间，是否激活标识位，初始化属性源(property source)配置
        prepareRefresh();

        // Tell the subclass to refresh the internal bean factory.
        // 2、告诉子类启动refreshBeanFactory()方法，
        // Bean定义资源文件的载入从子类的refreshBeanFactory()方法启动
        // 返回一个factory 为什么需要返回一个工厂? 因为要对工厂进行初始化 即第三步
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // Prepare the bean factory for use in this context.
        // 3、准备工厂，为BeanFactory配置容器特性，例如类加载器、事件处理器等
        prepareBeanFactory(beanFactory);

        try {
            // Allows post-processing of the bean factory in context subclasses.
            // 4、为容器的某些子类指定特殊的BeanPost事件处理器
            postProcessBeanFactory(beanFactory);

            // Invoke factory processors registered as beans in the context.
            // 5、在spring的环境中去执行所有注册的BeanFactoryPostProcessor的Bean
            // 设置执行自定义的ProcessBeanFactory 和spring内部自己定义的
            invokeBeanFactoryPostProcessors(beanFactory);

            // Register bean processors that intercept bean creation.
            // 6、为BeanFactory注册BeanPostProcessor事件处理器.
            // BeanPostProcessor是Bean后置处理器，用于监听容器触发的事件
            registerBeanPostProcessors(beanFactory);

            // Initialize message source for this context.
            // 7、初始化信息源，和国际化相关.
            initMessageSource();

            // Initialize event multicaster for this context.
            // 8、初始化容器应用事件传播器.
            initApplicationEventMulticaster();

            // Initialize other special beans in specific context subclasses.
            // 9、调用子类的某些特殊Bean初始化方法
            onRefresh();

            // Check for listener beans and register them.
            // 10、为事件传播器注册事件监听器.
            registerListeners();

            // Instantiate all remaining (non-lazy-init) singletons.
            // 11、初始化所有剩余的单例Bean
            finishBeanFactoryInitialization(beanFactory);

            // Last step: publish corresponding event.
            // 12、初始化容器的生命周期事件处理器，并发布容器的生命周期事件
            finishRefresh();
        }

        catch (BeansException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Exception encountered during context initialization - " +
                            "cancelling refresh attempt: " + ex);
            }

            // Destroy already created singletons to avoid dangling resources.
            destroyBeans();

            // Reset 'active' flag.
            cancelRefresh(ex);

            // Propagate exception to caller.
            throw ex;
        }

        finally {
            // Reset common introspection caches in Spring's core, since we
            // might not ever need metadata for singleton beans anymore...
            resetCommonCaches();
        }
    }
}
```

### 5.1 prepareRefresh();准备工作

准备工作包括设置启动时间，是否激活标识位，得到系统环境初始化属性源(property source)配置

这个方法其实不重要，就是去准备sprng的环境和bean的声明周期都没有一点关系，这个方法知道下就行了

```java
/**
	 * Prepare this context for refreshing, setting its startup date and
	 * active flag as well as performing any initialization of property sources.
	 */
	protected void prepareRefresh() {
		// Switch to active.
		this.startupDate = System.currentTimeMillis();
		this.closed.set(false);
		this.active.set(true);

		if (logger.isInfoEnabled()) {
			logger.info("Refreshing " + this);
		}

		// Initialize any placeholder property sources in the context environment.
		// 这个方法默认没有做任何事情，给子类去实现的
		initPropertySources();

		// Validate that all properties marked as required are resolvable:
		// see ConfigurablePropertyResolver#setRequiredProperties
		// 得到系统环境
		getEnvironment().validateRequiredProperties();

		// Store pre-refresh ApplicationListeners...
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// Reset local application listeners to pre-refresh state.
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// Allow for the collection of early ApplicationEvents,
		// to be published once the multicaster is available...
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}
```





======================================我是分隔符(*^__^*) ===================================





###5.2 告诉子类启动refreshBeanFactory()方法，得到bean工厂

```java
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    // 这里使用了委派设计模式，
    // 父类定义了抽象的refreshBeanFactory()方法，具体实现调用子类容器的refreshBeanFactory()方法
    refreshBeanFactory();
    // getBeanFactory()就是得到工厂
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (logger.isDebugEnabled()) {
        logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
    }
    return beanFactory;
}
```

这里xml方式的配置和javaConfig方式的配置的逻辑是相差很大的

#### 5.2.1.javaConfig的情况很简单

(1).告诉子类启动refreshBeanFactory()方法

由于我是通过AnnotationConfigApplicationContext 来创建spring的环境的，那么实际上refreshBeanFactory()

这个方法我会调用到GenericApplicationContext里，可以看实际上就是通过CAS标记bean工厂正在或已初始化

然后给我们的bean工厂设置了一个id

```java
@Override
	protected final void refreshBeanFactory() throws IllegalStateException {
		if (!this.refreshed.compareAndSet(false, true)) {
			throw new IllegalStateException(
					"GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
		}
		this.beanFactory.setSerializationId(getId());
	}
```

(2).得到bean工厂

这里会调用到父类GenericApplicationContext的getBeanFactory()直接返回一个DefaultListableBeanFactory工厂对象，那这个对象是在哪里被初始化的呢?

就是我们AnnotationConfigApplicationContext的构造方法里调用了父类GenericApplicationContext的构造方法中被初始化的

```java
@Override
public final ConfigurableListableBeanFactory getBeanFactory() {
    return this.beanFactory;
}
```

#### 5.2.2.xml的情况

(1).会调用到AbstractRefreshableApplicationContext里的refreshBeanFactory()

```java
/**
	 * This implementation performs an actual refresh of this context's underlying
	 * bean factory, shutting down the previous bean factory (if any) and
	 * initializing a fresh bean factory for the next phase of the context's lifecycle.
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		// 如果有容器，销毁容器中的bean，关闭容器
		if (hasBeanFactory()) {
			destroyBeans();
			closeBeanFactory();
		}
		try {
			// 创建IOC容器
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			beanFactory.setSerializationId(getId());
			// 对IOC容器进行定制化，如设置启动参数，开启注解的自动装配等
			customizeBeanFactory(beanFactory);
			// 调用载入Bean定义的方法，主要这里又使用了一个委派模式，
			// 在当前类中只定义了抽象的loadBeanDefinitions方法，具体的实现调用子类容器
			loadBeanDefinitions(beanFactory);
			synchronized (this.beanFactoryMonitor) {
				this.beanFactory = beanFactory;
			}
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}
```

可以看到如果是xml的情况，bean工厂是在这个方法里被创建的和设置一些基本参数的，返回的也是一个DefaultListableBeanFactory对象



(2).loadBeanDefinitions(beanFactory);

会调用到AbstractXmlApplicationContext里的loadBeanDefinitions()方法，就是去定义一个xml的bean读取器



我这里不去关注xml的情况





======================================我是分隔符(*^__^*) ===================================



###5.3 prepareBeanFactory(beanFactory);准备工厂，为BeanFactory配置容器特性，例如类加载器、事件处理器等

调用本类AbstractApplicationContext里的另一个方法

```java
/**
	 * Configure the factory's standard context characteristics,
	 * such as the context's ClassLoader and post-processors.
	 * @param beanFactory the BeanFactory to configure
	 *
	 * 配置其标准的特征，比如上下文的加载器ClassLoader和post-processors回调
	 * 此处的beanFactory参数等于DefaultListableFactory
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Tell the internal bean factory to use the context's class loader etc.
		// 配置classLoader
		beanFactory.setBeanClassLoader(getClassLoader());
		// 设置bean表达式解释器
		beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		// 设置属性注册解析器PropertyEditor 对象与string类型的转换   <property red="dao">
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// Configure the bean factory with context callbacks.
		/**
		 * 核心代码
		 * 添加一个后置管理器 ApplicationContextAwareProcessor
		 * 能够在bean中获得到各种*Aware（*Aware都有其作用）
		 */
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		// 添加自动注入被忽略的列表
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

		// BeanFactory interface not registered as resolvable type in a plain factory.
		// MessageSource registered (and found for autowiring) as a bean.
		// bean的替换 如果我们想自动注入以下类型的对象，就用后面的对象替换注入
        // 比如我想注入一个ApplicationContext对象，那么就把当前的上下文对象注入进来
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// Register early post-processor for detecting inner beans as ApplicationListeners.
        // 将实现了ApplicationListener接口的Bean添加到容器的监听器列表
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
        // 增加类加载期织入LTW(LoadTimeWeaving)的支持,AspectJ会用到
		if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
        /**
		 * 意思是如果自定义的Bean中没有名为"systemProperties"和"systemEnvironment"的Bean，
		 * 则注册两个Bena，Key为"systemProperties"和"systemEnvironment"，Value为Map，
		 * 这两个Bean就是一些系统配置和系统环境信息
		 */
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
	}
```

​	1.设置类加载器，bean表达式解释器, 属性解析注册器

2. beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

添加一个后置管理器 ApplicationContextAwareProcessor，放入到工厂类里的一个list当中

```java
// 存放bean后置处理器的list
// 在bean的实例化过程中会循环这个list依次来执行这list里面的后置处理器，达到插手bean的实例化过程
private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
```

这里要知道我们自己实现的后置处理器可以加上@Component来让spring扫描并加载注册到容器中，那么

spring自己内部的后置处理器如何添加到容器中呢?就是spring内部手动add的



ApplicationContextAwareProcessor中处理了多个xxxAware，比如实现ApplicationContextAware接口可以让我们在业务类实现该接口后拿到spring的上下文

具体的spring的后置处理器和扩展点的分析在这里就不做分析，可以看我的另外一个文档。

在bean的实例化过程中会循环这个list依次来执行这list里面的后置处理器，达到插手bean的实例化过程。



3.添加自动注入被忽略的列表

Spring提供的这些XxxxAware的接口我们是无法进行自动注入的，实现这些接口后，spring会在ApplicationContextAwareProcessor中做相应处理

4.添加BeanPostProcessor实现ApplicationListenerDetector，它是用来将实现了ApplicationListener接口的Bean添加到容器的监听器列表。

5.如果beanFactory中包含名称为loadTimeWeaver的Bean，则添加BeanPostProcessor实现：LoadTimeWeaverAwareProcessor，它是用来处理AspectJ类加载期织入LTW（Load Time Weaving）的。

具体关于LTW可以看看下面的文章

6.注册环境系统相关的bean（environment，systemProperties，systemEnvironment）

[https:// www.cnblogs.com/takumicx/p/10150344.html#%E5%85%B3%E4%BA%8Eloadtimeweaving](https:// www.cnblogs.com/takumicx/p/10150344.html#关于loadtimeweaving)





####5.3.1 ApplicationContextAwareProcessor 源码分析

作用: 能够在bean中获得到各种XxxxAware进行相应处理（XxxxAware都有其作用）

处理所有的Aware接口，进行如下操作：
如果bean实现了EnvironmentAware接口，调用bean.setEnvironment
如果bean实现了EmbeddedValueResolverAware接口，调用bean.setEmbeddedValueResolver
如果bean实现了ResourceLoaderAware接口，调用bean.setResourceLoader
如果bean实现了ApplicationEventPublisherAware接口，调用bean.setApplicationEventPublisher
如果bean实现了MessageSourceAware接口，调用bean.setMessageSource
如果bean实现了ApplicationContextAware接口，调用bean.setApplicationContext


```java
class ApplicationContextAwareProcessor implements BeanPostProcessor {

	private final ConfigurableApplicationContext applicationContext;

	private final StringValueResolver embeddedValueResolver;


	/**
	 * Create a new ApplicationContextAwareProcessor for the given context.
	 */
	public ApplicationContextAwareProcessor(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.embeddedValueResolver = new EmbeddedValueResolver(applicationContext.getBeanFactory());
	}


	@Override
	@Nullable
	public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
		AccessControlContext acc = null;

		if (System.getSecurityManager() != null &&
				(bean instanceof EnvironmentAware || bean instanceof EmbeddedValueResolverAware ||
						bean instanceof ResourceLoaderAware || bean instanceof ApplicationEventPublisherAware ||
						bean instanceof MessageSourceAware || bean instanceof ApplicationContextAware)) {
			acc = this.applicationContext.getBeanFactory().getAccessControlContext();
		}

		if (acc != null) {
			AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
				invokeAwareInterfaces(bean);
				return null;
			}, acc);
		}
		else {
			invokeAwareInterfaces(bean);
		}

		return bean;
	}

	private void invokeAwareInterfaces(Object bean) {
		if (bean instanceof Aware) {
			if (bean instanceof EnvironmentAware) {
				((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
			}
			if (bean instanceof EmbeddedValueResolverAware) {
				((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(this.embeddedValueResolver);
			}
			if (bean instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
			}
			if (bean instanceof ApplicationEventPublisherAware) {
				((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
			}
			if (bean instanceof MessageSourceAware) {
				((MessageSourceAware) bean).setMessageSource(this.applicationContext);
			}
			// spring帮你set一个applicationContext对象
			// 所以当我们自己的一个对象实现了ApplicationContextAware对象只需要提供setter就能得到applicationContext对象
			if (bean instanceof ApplicationContextAware) {
				((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
				// 下面代码是自己测试的，不是源码的一部分
				/*if (!bean.getClass().getSimpleName().equals("UserDao")) {
					((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
				}*/
			}
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}
```


#### 5.3.2 ApplicationContextAware 是如何能够给我们的业务类注入bean工厂的

首先我们想想看，在spring中提供了一个接口ApplicationContextAware，通过这个接口，我们可以在我们的业务类中获取spring的上下文

```java
public interface ApplicationContextAware extends Aware {

	/**
	 *  可以通过该方法可以获取applicationContext
	 */
	void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

```



通过ApplicationContextAware这个接口，我们可以在我们的业务类中获取spring的上下文

```java
@Service
public class UserService implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	public UserService() {
		System.out.println("UserService构造");
	}

	@PostConstruct
	public void init() {
		System.out.println("UserService init");
	}


	public void query() {
		System.out.println("query");
		applicationContext.getBean("xxx");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		System.out.println(applicationContext);
	}
}

```

那么是如何获取的呢? 我来从源码分析一下

我们看到ApplicationContextAwareProcessor中的源码，可以看到在ApplicationContextAwareProcessor中当你的bean实现了ApplicationContextAware时，spring会通过调用我们重写的setApplicationContext()方法将applicationContext传给你，所以我们才能在自己的业务类中得到spring的上下文，大家也可以尝试修改源码体验一下其是如何工作的。

当然同样的其他的XxxAware也是同样的工作原理。

```java
private void invokeAwareInterfaces(Object bean) {
		if (bean instanceof Aware) {
			if (bean instanceof EnvironmentAware) {
				((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
			}
			if (bean instanceof EmbeddedValueResolverAware) {
				((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(this.embeddedValueResolver);
			}
			if (bean instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
			}
			if (bean instanceof ApplicationEventPublisherAware) {
				((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
			}
			if (bean instanceof MessageSourceAware) {
				((MessageSourceAware) bean).setMessageSource(this.applicationContext);
			}
			// spring帮你set一个applicationContext对象
			// 所以当我们自己的一个对象实现了ApplicationContextAware对象只需要提供setter就能得到applicationContext对象
			if (bean instanceof ApplicationContextAware) {
				((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
				// 下面代码是自己测试的，不是源码的一部分
				/*if (!bean.getClass().getSimpleName().equals("UserDao")) {
					((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
				}*/
			}
		}
	}
```







======================================我是分隔符(*^__^*) ===================================





###5.4 允许上下文中的子类对bean工厂在创建与初始化完成后进行beanFactory的后置处理

这个方法通过Debug看到实际上Spring并没有去做具体的实现

![1584461331698](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584461331698.png)



看了一下其子类发现都和web相关，也就是需要结合web环境的情况下，才会走这些实现，因此这里暂不做分析

![1584461275262](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584461275262.png)

但是我网上查阅了资料大家可以看看

https:// blog.csdn.net/qq_28802119/article/details/82908592





======================================我是分隔符(*^__^*) ===================================





###5.5 invokeBeanFactoryPostProcessors(beanFactory); 在spring的环境中去执行所有注册的BeanFactoryPostProcessor的Bean

这个方法会在spring的环境中去执行bean工厂的后置处理器，包括spring内部的和我们自定义的

下面我们通过代码来进行分析

```java
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// 这个地方需要注意getBeanFactoryPostProcessors()是获取手动给spring的BeanFactoryPostProcessor
		// 自定义并不仅仅是程序员自己写的
		// 自己写的可以加companent也可以不加
		// 如果加了getBeanFactoryPostProcessors()这个地方得不到，是spring自己扫描的
		// 为什么得不到getBeanFactoryPostProcessors（）这个方法是直接获取一个list，
		// 这个list是在AnnotationConfigApplicationContext被定义
		// 所谓的自定义的就是你手动调用AnnotationConfigApplicationContext.addBeanFactoryPostProcesor();
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}
```

1. 执行所有的BeanDefinitionRegistryPostProcessor和BeanFactoryPostProcessors

2. 执行完后为Spring的beanFactory添加两个bean的后置处理器

   

####5.5.1.getBeanFactoryPostProcessors() 获取的是什么?

！！！那么一定要注意的是，这里我说的自定义的BeanFactoryPostProcessor是没有去交给了spring容器去管理的，而是我们**在执行refresh()方法之前手动调用**AnnotationConfigApplicationContext.addBeanFactoryPostProcesor();方法添加的

为什么?

我们来看下getBeanFactoryPostProcessors()方法，他是直接返回了一个叫beanFactoryPostProcessors的list。

```java
public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
}
```

```java
/** BeanFactoryPostProcessors to apply on refresh */
private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
```

你想想这个阶段Spring根本还没来进行解析我的配置类，还没有来扫描我这个bean，那么我这个类其实对于Spring来说根本就不存在，那么当然不会执行的。

只有当我们在refresh()方法之前手动调用AnnotationConfigApplicationContext.addBeanFactoryPostProcesor();方法添加这里才会获取到

![1584463742959](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584463742959.png)

![1584463669104](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584463669104.png)







####5.5.2  什么是BeanDefinitionRegistryPostProcessor?

Spring的bean工厂后置处理器BeanFactoryPostProcessor实际上还有一个扩展接口BeanDefinitionRegistryPostProcessor



这个具体的解释和例子看我的另一个博文Spring中的5个扩展点，这里大概说一下BeanDefinitionRegistryPostProcessor继承了BeanFactoryPostProcessor，是对BeanFactoryPostProcessor的一个扩展，可以实现bean的动态注册，那么我们从上面的源码里面看到BeanDefinitionRegistryPostProcessor中扩展的方法是优先于BeanFactoryPostProcessor中的方法执行的。

这个接口的作用是可以实现动态向Spring中注册bean，具体例子也可以参考我的另一个博文Spring中的5个扩展点，我这里就不再多说。

```java
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

	/**
	 * Modify the application context's internal bean definition registry after its
	 * standard initialization. All regular bean definitions will have been loaded,
	 * but no beans will have been instantiated yet. This allows for adding further
	 * bean definitions before the next post-processing phase kicks in.
	 * @param registry the bean definition registry used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;
```



####5.5.3 BeanFactoryPostProcessor 执行顺序

1.先执行我们就是直接注册的BeanDefinitionRegistryPostProcessor中的postProcessBeanDefinitionRegistry()方法

2.执行Spring自己内部的BeanDefinitionRegistryPostProcessor中的postProcessBeanDefinitionRegistry()方法

3.然后执行我们自己定义的(被spring扫描出来的)BeanDefinitionRegistryPostProcessor且实现了Ordered接口的，也就是先执行排过序的，当然也是执行postProcessBeanDefinitionRegistry()方法

4.执行剩余的BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry()方法

5.执行所有BeanDefinitionRegistryPostProcessor中的postProcessBeanFactory()方法

6.执行我们自定义的BeanFactoryPostProcessor 



#####5.5.3.1 那Spring为什么要设计成我们直接注册的最先执行呢?

我的想法是，首先如果Spring没有对外提供扩展接口，那么Spring的bean工程生产bean的过程对于我们使用者来说就是一个固定的过程，就是因为过程固定了，所以Spring才要提供出扩展接口，让我们对bean工厂做一些后继处理。

那么仅仅是提供出接口我们就能插手bean工厂的一些事情了吗? 很显然不是，首先Spring内部自己也用到了这些扩展接口，那么我们仔细想想，这些Spring内部扩展的不就是Spring工厂生产bean流程固定的一部分，那为什么Spring要把这些逻辑抽离出来?  就是为了可以让我们在这些逻辑的前后去插手一些事情。

我认为，在现代的编程越来越是语义化的编程，那么我们在调用refresh()方法之前直接注册这些我们自定义的后继处理器，然后才来初始化Spring的环境，也就是说在你造工厂之前，我就告诉了Spring我要插手你造工厂，从语义上来说，我们这个插手的过程就是要优先执行的，而我其他自定义的后继处理器是通过加了@Component注解的，那么你想想如果我这个工厂都没建造完成，我怎么来处理你这些类? 至少也要等我工厂最基本的模型建造完成后才能来处理吧? 因此，从语义上来说，我们通过加了注解的后继处理器就应该在Spring自己内部的逻辑执行完后才来执行。实际上，Spring也只有在执行完他内部的逻辑后才能来执行我们通过注解标注的后继处理器。

因为所谓的内部逻辑，其实就是来解析我们的Spring配置类。



下面我来结合源码分析一下



####5.5.4 开始处理Bean工厂的后置处理器PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

这段代码逻辑相对较为复杂



```java
/**
	 * 这里面有3个list
	 * List<BeanFactoryPostProcessor> regularPostProcessors
	 * 放我们手动添加的BeanFactoryPostProcessor
	 *
	 * List<BeanDefinitionRegistryPostProcessor> registryProcessors
	 * 一开始放我们直接注册的BeanDefinitionRegistryPostProcessor，后面会把spring内部的也合并过来,
	 * 用于最后执行BeanFactoryPostProcessor的回调
	 *
	 * List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors
	 * 	放的是要去执行的实现了BeanDefinitionRegistryPostProcessor接口的对象
	 * 	什么意思? BeanDefinitionRegistryPostProcessor的执行顺序是
	 * 	执行完我们直接注册的后，先执行Spring自己内部的，然后再执行我们自定义的，
	 * 	所以一开始放的是Spring内部自己实现了BeanDefinitionRegistryPostProcessor接口的对象，去执行
	 * 	然后清除，再遍历放入我们自己的，再去执行，然后清除这个list
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();

		// 如果实现了BeanDefinitionRegistry接口
		if (beanFactory instanceof BeanDefinitionRegistry) {
			// 就将beanFactory转为BeanDefinitionRegistry类型
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// 放我们直接注册的BeanFactoryPostProcessor
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 放我们直接注册的BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 遍历我们手动add添加的的beanFactoryPostProcessors，进行分类加入到上面的两个list中
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					// 如果实现BeanDefinitionRegistryPostProcessor
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 执行我们手动add的BeanDefinitionRegistryPostProcessor中的
					// postProcessBeanDefinitionRegistry方法的回调
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					// 如果实现BeanfactoryPostProcessor
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			/**
			 * 这个currentRegistryProcessors
			 * 放的是要去执行的实现了BeanDefinitionRegistryPostProcessor接口的对象
			 *
			 * 什么意思? BeanDefinitionRegistryPostProcessor的执行顺序是
			 * 先执行Spring自己内部的，然后再执行我们自定义的，
			 * 所以一开始放的是Spring内部自己实现了BeanDefinitionRegistryPostProcessor接口的对象，去执行
			 * 然后清除，再遍历放入我们自己的，再去执行，然后清除这个list
			 */
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			// getBeanNamesForType  根据bean的类型获取bean的名字ConfigurationClassPostProcessor
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			/**
			 *
			 * 这个地方可以得到一个BeanFactoryPostProcessor，因为是spring默认在最开始自己注册的
			 * 为什么要在最开始注册这个呢？
			 * 因为spring的工厂需要去解析去扫描等等功能
			 * 而这些功能都是需要在spring工厂初始化完成之前执行
			 * 要么在工厂最开始的时候、要么在工厂初始化之中，反正不能再之后
			 * 因为如果在之后就没有意义，因为那个时候已经需要使用工厂了
			 * 所以这里spring'在一开始就注册了一个BeanFactoryPostProcessor，用来插手springfactory的实例化过程
			 * 在这个地方断点可以知道这个类叫做ConfigurationClassPostProcessor
			 * ConfigurationClassPostProcessor那么这个类能干嘛呢？可以参考源码
			 * 而且这里要注意，这边调用了getBean，Spring在这边通过getBean直接将其创建出来
			 */
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);

				}
			}
			// 排序不重要，况且currentRegistryProcessors这里也只有一个数据
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 合并list，将spring内部的和我们自定义的合并到一起，
			// 这个list是最后用于执行BeanFactoryPostProcessor的回调
			registryProcessors.addAll(currentRegistryProcessors);

			// 这里先执行Spring内部的BeanDefinitionRegistryPostProcessor
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// ======执行完成我们手动add的和Spring内部的BeanDefinitionRegistryPostProcessor===

			// 清除list的里数据，接下来放入其他要执行的BeanDefinitionRegistryPostProcessor
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 调用我们自己实现了Ordered的BeanDefinitionRegistryPostProcessors，也就是优先执行有排序的
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 循环中断标记 这个循环就是来处理我们自定义的BeanDefinitionRegistryPostProcessors
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						// 遍历取出我们自定义的BeanDefinitionRegistryPostProcessors 放入list
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				// 合并到registryProcessors用于最后执行BeanFactoryPostProcessor的回调
				registryProcessors.addAll(currentRegistryProcessors);
				// 执行我们自定义的BeanDefinitionRegistryPostProcessors
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			/**
			 * 执行BeanFactoryPostProcessor的回调
			 * 这是执行的是BeanFactoryPostProcessor的postProcessBeanFactory回调
			 */
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// 执行我们自定义BeanFactoryPostProcessor的postProcessBeanFactory回调
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}
```



**首先这里面，Spring要来执行BeanFactoryPostProcessor ，就需要将所有的BeanFactoryPostProcessor 实例化出来，因为只有实例化了才能使用这个对象，那Spring就是通过getBean()方法来进行实例化的。当然我们自己直接注册的就不需要进行实例化了，因为我们直接注册的就是new出来给了Spring的**



**其次这里面定义了3个list 目的就是要为了实现我上面说的BeanFactoryPostProcessor 执行顺序**

List<BeanFactoryPostProcessor> regularPostProcessors

放我们就是直接注册的BeanFactoryPostProcessor



BeanFactoryPostProcessorList<BeanDefinitionRegistryPostProcessor> registryProcessors 

一开始放我们手动添加的BeanDefinitionRegistryPostProcessor，**后面会把spring内部的也合并过来,用于最后执行BeanFactoryPostProcessor的回调**



List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessor   

放的是要去执行的实现了BeanDefinitionRegistryPostProcessor接口的对象   

什么意思? BeanDefinitionRegistryPostProcessor的执行顺序是 :**执行完我们直接注册的后，先执行Spring自己内部的，然后再执行我们自定义的**，所以一开始放的是Spring内部自己实现了BeanDefinitionRegistryPostProcessor接口的对象，去执行， 然后清除，再遍历放入我们自己的，再去执行，然后清除这个list



**执行逻辑===>**

1.if (beanFactory instanceof BeanDefinitionRegistry)

实际上这个if判断条件肯定是成立的，因为这个类型都是Spring自己内部获取的，不是我们来决定的。

一开始在初始化bean工厂的时候的类型是DefaultListableBeanFactory，所以这里我们的beanFactory肯定是实现了BeanDefinitionRegistry，大家看下类关系图:

![1584515831355](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584515831355.png)

2.将beanFactory转为BeanDefinitionRegistry类型，创建两个list

```java
// 放我们手动直接注册的BeanFactoryPostProcessor
List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
// 放我们手动直接注册的BeanDefinitionRegistryPostProcessor
List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
```

3.遍历我们手动直接注册的的beanFactoryPostProcessors，进行分类加入到上面的两个list中, 如果是BeanDefinitionRegistryPostProcessor类型的就在添加到list之前执行其postProcessBeanDefinitionRegistry()回调

4.又创建了一个list，用于存放去执行BeanDefinitionRegistryPostProcessor的回调方法的对象

```java
List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();
```

5.获取Spring内部的BeanDefinitionRegistryPostProcessor并通过getBean()方法实例化，实际上就是获得一个ConfigurationClassPostProcessor对象用于解析配置类,这个bean是一开始注册的6个bean之一，这个类的执行逻辑我下面会重点分析



6.执行ConfigurationClassPostProcessor的回调, 到此我们手动add的和Spring内部的BeanDefinitionRegistryPostProcessor都执行完成(注意只是执行postProcessBeanDefinitionRegistry()这个方法), 将currentRegistryProcessors合并到registryProcessors这个list中，然后清空currentRegistryProcessors

```java
invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
```



7.，然后通过getBean()方法实例化我们自己实现了PriorityOrdered接口的BeanDefinitionRegistryPostProcessors并调用，也就是优先执行有排序的，将currentRegistryProcessors合并到registryProcessors这个list中，然后清空currentRegistryProcessors

8.循环处理我们自定义的BeanDefinitionRegistryPostProcessors，将currentRegistryProcessors合并到registryProcessors这个list中，然后清空currentRegistryProcessors

到这里所有BeanDefinitionRegistryPostProcessor的回调postProcessBeanDefinitionRegistry()都执行完成。

9.最后就是来处理所有的BeanFactoryPostProcessor的回调，注意是先执行了BeanDefinitionRegistryPostProcessor类型的，再最后执行我们自定义的

```java
// 执行所有BeanDefinitionRegistryPostProcessor的postProcessBeanFactory回调
invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
// 执行我们自定义BeanFactoryPostProcessor的postProcessBeanFactory回调
invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
```

到此该方法的逻辑就分析完成，但是这里面有一个超级重要的ConfigurationClassPostProcessor的执行还没分析，我接下来要分析的就是这个类的回调逻辑





####5.5.5  解析配置类 基于已注册的全注解类或半注解类来构建和验证一个配置模型

####ConfigurationClassPostProcessor类的postProcessBeanDefinitionRegistry()方法执行逻辑



下面就是Spring初始化工厂时的重点，Spring开始解析我们的配置文件，这段逻辑非常复杂，各种套娃方法

```java
@Override
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    int registryId = System.identityHashCode(registry);
    if (this.registriesPostProcessed.contains(registryId)) {
        throw new IllegalStateException(
            "postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
    }
    if (this.factoriesPostProcessed.contains(registryId)) {
        throw new IllegalStateException(
            "postProcessBeanFactory already called on this post-processor against " + registry);
    }
    this.registriesPostProcessed.add(registryId);
	// 做了校验后调用了这个方法
    processConfigBeanDefinitions(registry);
}

```



可以看到实际上是做了一些校验后调用了另一个真正干活的方法processConfigBeanDefinitions(registry);



```java
/**
	 * Build and validate a configuration model based on the registry of
	 * {@link Configuration} classes.
	 * 基于已注册的全注解类或半注解类来构建和验证一个配置模型
	 */
public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
    // 定义一个list存放配置类 提供的BeanDefinition（项目当中提供了@Compent）
    List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
    // 获取容器中注册的所有BeanDefinition名字
    // 7个
    String[] candidateNames = registry.getBeanDefinitionNames();

    /**
		 * Full 全注解类型 加了@Configuration
		 * Lite 半注解类型 ComponentScan Component Import ImportResource
		 */
    for (String beanName : candidateNames) {
        BeanDefinition beanDef = registry.getBeanDefinition(beanName);
        if (ConfigurationClassUtils.isFullConfigurationClass(beanDef) ||
            ConfigurationClassUtils.isLiteConfigurationClass(beanDef)) {
            // 如果BeanDefinition中的configurationClass属性为full或者lite,则意味着已经处理过了,直接跳过
            // 这里需要结合下面的代码才能理解
            if (logger.isDebugEnabled()) {
                logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
            }
        }
        // 判断是否是Configuration类，如果加了Configuration下面的这几个注解就不再判断了
        // 还有  add(Component.class.getName());
        // 		candidateIndicators.add(ComponentScan.class.getName());
        // 		candidateIndicators.add(Import.class.getName());
        // 		candidateIndicators.add(ImportResource.class.getName());
        // beanDef == appconfig
        else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
            // BeanDefinitionHolder 也可以看成一个数据结构
            configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
        }
    }

    // Return immediately if no @Configuration classes were found
    // 如果没有配置类就直接返回
    if (configCandidates.isEmpty()) {
        return;
    }

    // Sort by previously determined @Order value, if applicable
    // 排序，根据@Order,不重要
    configCandidates.sort((bd1, bd2) -> {
        int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
        int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
        return Integer.compare(i1, i2);
    });

    // Detect any custom bean name generation strategy supplied through the enclosing application context
    SingletonBeanRegistry sbr = null;
    // 如果BeanDefinitionRegistry是SingletonBeanRegistry子类的话,
    // 由于我们当前传入的是DefaultListableBeanFactory,是SingletonBeanRegistry 的子类
    // 因此会将registry强转为SingletonBeanRegistry
    if (registry instanceof SingletonBeanRegistry) {
        sbr = (SingletonBeanRegistry) registry;
        if (!this.localBeanNameGeneratorSet) {// 判断是否有自定义的beanName生成器
            BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
            // SingletonBeanRegistry中有id为 org.springframework.context.annotation.internalConfigurationBeanNameGenerator
            // 如果有则利用他的，否则则是spring默认的
            if (generator != null) {
                this.componentScanBeanNameGenerator = generator;
                this.importBeanNameGenerator = generator;
            }
        }
    }

    // Parse each @Configuration class
    // 实例化ConfigurationClassParser 为了解析各个配置类
    if (this.environment == null) {
        this.environment = new StandardEnvironment();
    }

    // Parse each @Configuration class
    // 实例化ConfigurationClassParser 为了解析各个配置类
    ConfigurationClassParser parser = new ConfigurationClassParser(
        this.metadataReaderFactory, this.problemReporter, this.environment,
        this.resourceLoader, this.componentScanBeanNameGenerator, registry);

    // 实例化2个set,candidates用于将之前加入的configCandidates进行去重
    // 因为可能有多个配置类重复了
    Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
    // alreadyParsed用于判断是否处理过 即candidates中处理过的就往alreadyParsed中扔
    Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
    do {
        // 解析配置类 ==> 扫描得到所有的@Component的bd ==>处理所有@Import
        parser.parse(candidates);
        parser.validate();

        // 获取通过@Import，@Bean注解等解析得到的类 (也就是除去了@Component注解的bean)
        Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
        configClasses.removeAll(alreadyParsed);

        // Read the model and create bean definitions based on its content
        if (this.reader == null) {
            this.reader = new ConfigurationClassBeanDefinitionReader(
                registry, this.sourceExtractor, this.resourceLoader, this.environment,
                this.importBeanNameGenerator, parser.getImportRegistry());
        }
        /**
			 * 这里值得注意的是扫描出来的bean当中可能包含了特殊类
			 * 比如ImportBeanDefinitionRegistrar那么也在这个方法里面处理
			 * 但是并不是包含在configClasses当中
			 * configClasses当中主要包含的是importSelector
			 * 因为ImportBeanDefinitionRegistrar在扫描出来的时候已经被添加到一个list当中去了
			 */
        // bd 到 map 除却普通
        this.reader.loadBeanDefinitions(configClasses);
        alreadyParsed.addAll(configClasses);

        candidates.clear();
        // 由于我们这里进行了扫描，把扫描出来的BeanDefinition注册给了factory
        if (registry.getBeanDefinitionCount() > candidateNames.length) {
            String[] newCandidateNames = registry.getBeanDefinitionNames();
            Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
            Set<String> alreadyParsedClasses = new HashSet<>();
            for (ConfigurationClass configurationClass : alreadyParsed) {
                alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
            }
            for (String candidateName : newCandidateNames) {
                if (!oldCandidateNames.contains(candidateName)) {
                    BeanDefinition bd = registry.getBeanDefinition(candidateName);
                    if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
                        !alreadyParsedClasses.contains(bd.getBeanClassName())) {
                        candidates.add(new BeanDefinitionHolder(bd, candidateName));
                    }
                }
            }
            candidateNames = newCandidateNames;
        }
    }
    while (!candidates.isEmpty());

    // Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
    if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
        sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
    }

    if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
        // Clear cache in externally provided MetadataReaderFactory; this is a no-op
        // for a shared cache since it'll be cleared by the ApplicationContext.
        ((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
    }
}
```



代码太多我拆分出来分析========>

1.定义一个list存放配置类提供的BeanDefinition（项目当中提供了@Compent）,然后获取容器中注册的所有beanName(我这里就是6个spring内部的 + 我传入的一个配置类)

2.循环这些beanName，根据beanName获取BeanDefinition

3.判断BeanDefinition中的configurationClass属性是否为full或者lite，这句话看不懂没关系，要结合下面的才能看懂

​	3.1是则意味着已经处理过了,直接跳过

​    3.2如果没有被处理过 ，判断是不是Configuration类，如果是就加入到一个list中，怎么判断的我下面来分析



4.设置beanName生成器

5.实例化2个set,candidates用于将之前加入的configCandidates进行去重

```java
// 因为可能有多个配置类重复了
Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
// alreadyParsed用于判断是否被解析过,即candidates中解析过的就往alreadyParsed中扔
Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
```

6.中间当然还做了一些事情，但不重要，大家看我源码里的注释就行，重要的就是得到配置类的集合后来循环解析配置类parser.parse(candidates); 

​	解析配置类的时候，如果是加了@Component注解的configurationClass会直接进行注册，其他的configurationClass则会放入到一个map集合中，等全部解析完配置文件后再进行注册。

7.获取通过@Import，@Bean注解等解析得到的ConfigurationClass (也就是除去了@Component注解的bean)

8.将这些ConfigurationClass 注册到bean工厂的beanDefinition map中





#####5.5.5.1 Spring如何判断一个bean是不是配置类?  由此引出什么是full或者lite(全注解类或半注解配置类)

下面我先来分析Spring是如果判断一个bean是不是配置类的？我们进入checkConfigurationClassCandidate()方法

 至于什么是full或者lite(全注解类或半注解类)，这个要等postProcessBeanDefinitionRegistry()逻辑分析完后我再来分析。



```java
/**
	 * Check whether the given bean definition is a candidate for a configuration class
	 * (or a nested component class declared within a configuration/component class,
	 * to be auto-registered as well), and mark it accordingly.
	 * @param beanDef the bean definition to check
	 * @param metadataReaderFactory the current factory in use by the caller
	 * @return whether the candidate qualifies as (any kind of) configuration class
	 */
	public static boolean checkConfigurationClassCandidate(BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
		String className = beanDef.getBeanClassName();
		if (className == null || beanDef.getFactoryMethodName() != null) {
			return false;
		}

		AnnotationMetadata metadata;
		/**
		 * beanDef instanceof AnnotatedBeanDefinition 他表示是加了注解的bean,
		 * spring容器在初始化时内部的bean是被解析成了RootBeanDefinition
		 * 因此在我们这个例子中，只有AppConfig才会进来
		 */
		if (beanDef instanceof AnnotatedBeanDefinition &&
				className.equals(((AnnotatedBeanDefinition) beanDef).getMetadata().getClassName())) {
			// Can reuse the pre-parsed metadata from the given BeanDefinition...
			// 如果BeanDefinition 是 AnnotatedBeanDefinition的实例,并且className 和 BeanDefinition中 的元数据 的类名相同
			// 则直接从BeanDefinition 获得Metadata
			metadata = ((AnnotatedBeanDefinition) beanDef).getMetadata();
		}
		else if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
			// Check already loaded Class if present...
			// since we possibly can't even load the class file for this Class.
			// 如果BeanDefinition 是 AbstractBeanDefinition的实例,并且beanDef 有 beanClass 属性存在
			// 则实例化StandardAnnotationMetadata
			Class<?> beanClass = ((AbstractBeanDefinition) beanDef).getBeanClass();
			metadata = new StandardAnnotationMetadata(beanClass, true);
		}
		else {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
				metadata = metadataReader.getAnnotationMetadata();
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find class file for introspecting configuration annotations: " + className, ex);
				}
				return false;
			}
		}

		// 判断当前这个bd中存在的类是不是加了@Configruation注解
		// 如果存在则spring认为他是一个全注解的类(即加了@Configruation的类)
		if (isFullConfigurationCandidate(metadata)) {
			// 如果存在Configuration 注解,则为BeanDefinition 设置configurationClass属性为full
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
		}
        // 如果不存在@Configuration注解，spring则认为是一个部分注解类
		// 判断是否加了以下注解，摘录isLiteConfigurationCandidate的源码
		//     candidateIndicators.add(Component.class.getName());
		// 		candidateIndicators.add(ComponentScan.class.getName());
		// 		candidateIndicators.add(Import.class.getName());
		// 		candidateIndicators.add(ImportResource.class.getName());
        // 或者如果没有加注解，但有方法加了@Bean注解
		else if (isLiteConfigurationCandidate(metadata)) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
		}
		else {
			return false;
		}

		// It's a full or lite configuration candidate... Let's determine the order value, if any.
		Integer order = getOrder(metadata);
		if (order != null) {
			beanDef.setAttribute(ORDER_ATTRIBUTE, order);
		}

		return true;
	}
```

这个方法的作用就是预解析已经注册到容器中的bean，判断是否是一个配置类，如果是配置类，是全配置类还是半配置类



1.取出传入的BeanDefinition中的类名，做校验用

2.取出传入的BeanDefinition的元数据

​	2.1 如果传入的BeanDefinition是AnnotatedBeanDefinition类型的，就强转为AnnotatedBeanDefinition后取			出元数据

​	2.2 如果传入的BeanDefinition是AbstractBeanDefinition类型的，就强转为AbstractBeanDefinition后取			出元数据

​	2.3如果两个都不是也取出元数据

​	Spring内部的bean是AbstractBeanDefinition类型的, 其实这里我们只需要关注我们自己的配置类的时候，

​	首先我们的配置类在最初被注册进Spring容器中的时候是被封装成AnnotatedBeanDefinition的，因此我们的配 置类对于Spring来说就是一个被注解标注了的BeanDefinition



3.判断是否被加了@Configruation注解

​	3.1如果加了则spring认为他是一个全注解的类(即加了@Configruation的类)，则设置configurationClass属性为full

​	3.2 如果没加@Configruation注解，Spring就去判断是否加了@Component，@ComponentScan，@Import，@ImportResource这几个注解或者有方法加了@Bean注解，那么spring则认为是一个部分注解类，设置configurationClass属性为lite，至于这是什么意思我后面再来分析



======================================我是分隔符(*^__^*) ===================================



#####5.5.5.2  Spring如何判断是一个半注解配置类?

如果没有加@Configuration注解，接下来会进入下面的方法判断是否是一个半注解配置类

```java
public static boolean isLiteConfigurationCandidate(AnnotationMetadata metadata) {
		// Do not consider an interface or an annotation...
		// 如果是个接口直接返回false
		if (metadata.isInterface()) {
			return false;
		}

		// Any of the typical annotations found?
		/**
		 * 如果没有加@Configuration注解，spring就认为该类是一个部分注解类
		 * 循环判断是否加了以下的注解
		 * candidateIndicators ==> Import Componet ImportResource ComponentScan
		 */
		for (String indicator : candidateIndicators) {
			if (metadata.isAnnotated(indicator)) {
				return true;
			}
		}

		// Finally, let's look for @Bean methods...
		try {
			// 判断方法是否有加@Bean注解
			return metadata.hasAnnotatedMethods(Bean.class.getName());
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to introspect @Bean methods on class [" + metadata.getClassName() + "]: " + ex);
			}
			return false;
		}
	}
```

我们看到里面遍历了一个candidateIndicators，然后判断该类是否有@Component，@ComponentScan，@Import，@ImportResource这几个注解

```java
private static final Set<String> candidateIndicators = new HashSet<>(8);
static {
    candidateIndicators.add(Component.class.getName());
    candidateIndicators.add(ComponentScan.class.getName());
    candidateIndicators.add(Import.class.getName());
    candidateIndicators.add(ImportResource.class.getName());
}
```

至于全注解配置类和半注解配置类的区别，我后面会分析到，这里暂时先不说



======================================我是分隔符(*^__^*) ===================================



#####5.5.5.3 解析配置文件 parser.parse(candidates);

当Spring循环判断完已经注册的bean是不是配置类后，会把全部的配置类加入到一个list中，

然后循环调用parser.parse(candidates)方法来解析配置类

```java
public void parse(Set<BeanDefinitionHolder> configCandidates) {
		this.deferredImportSelectors = new LinkedList<>();
		// 根据BeanDefinition 的类型 做不同的处理,一般都会调用ConfigurationClassParser#parse 进行解析
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				if (bd instanceof AnnotatedBeanDefinition) {
					// 解析注解对象，并且把解析出来的bd放到map，但是这里的bd指的是普通的
					// 何谓不普通的呢？比如@Bean 和各种beanFactoryPostProcessor得到的bean不在这里put
					// 但是这里解析，只是不put而已
					parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
				}
				else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
			}
		}

		// 处理延迟加载的importSelector 为什么要延迟加载?
		// 如果需要在所有的@Configuration处理完再导入时可以实现DeferredImportSelector接口
		processDeferredImportSelectors();
	}
```

1.new了一个LinkedList用于存放延迟加载的ImportSelector

```
this.deferredImportSelectors = new LinkedList<>();
```

2.这里实际是调用了parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());来解析配置类，并把解析出来的普通的beanDefinition放入到一个map中。

不普通的就是@Bean和各种beanFactoryPostProcessor得到的bean，这些bean也会被解析，但是不会被放入到这个map中



3.解析完配置类后，处理延迟加载的importSelector，在第一步里解析配置文件的时候会将需要延迟加载的importSelect放入到linkedList中，等全部解析完后，再来解析deferredImportSelector。这个的作用在SpringBoot里的自动配置中体现出来。



我接着点进解析方法：

```java
protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
		if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}

		// 处理重复Imported 的情况
		// 就是当前这个注解类有没有被别的类import
		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
		if (existingClass != null) {
			if (configClass.isImported()) {
				if (existingClass.isImported()) {
					existingClass.mergeImportedBy(configClass);
				}
				// Otherwise ignore new imported config class; existing non-imported class overrides it.
				return;
			}
			else {
				// Explicit bean definition found, probably replacing an import.
				// Let's remove the old one and go with the new one.
				this.configurationClasses.remove(configClass);
				this.knownSuperclasses.values().removeIf(configClass::equals);
			}
		}

		// Recursively process the configuration class and its superclass hierarchy.
		SourceClass sourceClass = asSourceClass(configClass);
		do {
			// 解析配置类
			sourceClass = doProcessConfigurationClass(configClass, sourceClass);
		}
		while (sourceClass != null);
		// 一个map，用来存放扫描出来的bean（注意这里的bean不是对象，仅仅bean的信息，因为还没到实例化这一步）
		this.configurationClasses.put(configClass, configClass);
	}
```

1.就是做了个判断是否该类已经被其他类Import过了，就不需要重复处理了，判断方式就是处理完的配置类都会被放入一个map中，从map中根据当前类去取，如果取到就说明被重复Import了

2.然后调用doProcessConfigurationClass()方法进行解析

3.解析完后放入到一个map中



下面我们点进真正的解析方法

```java
@Nullable
	protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
			throws IOException {
		// Recursively process any member (nested) classes first
		// 处理内部类,如果内部类也是个配置类也会进行解析，解析调用的方法逻辑是一样
		processMemberClasses(configClass, sourceClass);

		// Process any @PropertySource annotations
        // 处理@PropertySource注解，将解析出来的属性资源添加到environment
		for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), PropertySources.class,
				org.springframework.context.annotation.PropertySource.class)) {
			if (this.environment instanceof ConfigurableEnvironment) {
				processPropertySource(propertySource);
			}
			else {
				logger.warn("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
						"]. Reason: Environment must implement ConfigurableEnvironment");
			}
		}

		// Process any @ComponentScan annotations
        // 处理@ComponentScan注解,通过ComponentScanAnnotationParser解析@ComponentScan注解
		Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
		if (!componentScans.isEmpty() &&
				!this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
			for (AnnotationAttributes componentScan : componentScans) {
				// The config class is annotated with @ComponentScan -> perform the scan immediately
				// 扫描普通类=componentScan=com.bighao
				// 这里扫描出来所有@Component
				// 并且把扫描的出来的普通bean放到map当中
				Set<BeanDefinitionHolder> scannedBeanDefinitions =
						this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
				// Check the set of scanned definitions for any further config classes and parse recursively if needed
				// 检查扫描出来的类当中是否还有configuration
				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
					BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
					if (bdCand == null) {
						bdCand = holder.getBeanDefinition();
					}
					// 检查  todo
					if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
						parse(bdCand.getBeanClassName(), holder.getBeanName());
					}
				}
			}
		}

		/**
		 * 上面的代码就是扫描普通类----@Component
		 * 并且放到了bd map当中
		 */

		// Process any @Import annotations
		/**
		 * 下面processImports() 处理@Import  imports 3种情况
		 * ImportSelector
		 * import普通类 即import(xxx.class)，或者是ImportSelector返回的普通类
		 * ImportBeanDefinitionRegistrar
		 *
		 * 这里和内部的递归用时的情况不同
		 */
		/**
		 * 这里处理的import是需要判断我们的类当中时候有@Import注解
		 * 如果有这把@Import当中的值拿出来，是一个类
		 * 比如@Import(xxxxx.class)，那么这里便把xxxxx传进去进行解析
		 * 在解析的过程中如果发觉是一个importSelector那么就回调selector的方法
		 * 返回一个字符串（类名），通过这个字符串得到一个类
		 * 继而在递归调用本方法来处理这个类
		 *
		 *
		 * 为什么要单独写这么多注释来说说明这个类?
		 * 因为selector返回的那个类，严格意义上来讲不符合@Import(xxxx.class)，因为这个类没有被直接import
		 * 如果不符合，就不会调用这个getImports(sourceClass)方法，即得到所有的import的类
		 * 但是注意的是递归当中是没有getImports(sourceClass)的，意思是直接把selector当中返回的类直接当成
		 * 一个import的类去解析
		 * 总之就是一句话，@Import(xxx.class)，那么这个类会被解析
		 * 如果xxx是selector，那么它当中返回的类虽然没有直接加上@Import，但是也会直接解析
		 *
		 * 或者这么看 第一次来解析@Import注解的时候，通过getImports(sourceClass)来获取@Import(xxx.class)
		 * 里的类，进去后会有递归调用，递归的时候是不会调用getImports(sourceClass)的
		 * processImports()这个方法只是来把你传进来的类来判断是import的3种情况的哪一种，然后进行处理，
		 * 这时这些bean会暂时放到configurationClasses这个集合中，等全部解析完后再注册到bd map中
		 */
		// 判断一组类是不是imports（3种import）
		processImports(configClass, sourceClass, getImports(sourceClass), true);

        
		// Process any @ImportResource annotations
        // 处理@ImportResource注解：获取@ImportResource注解的locations属性，得到资源文件的地址信息
    // 然后遍历这些资源文件并把它们添加到配置类的importedResources属性中
		AnnotationAttributes importResource =
				AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
		if (importResource != null) {
			String[] resources = importResource.getStringArray("locations");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			for (String resource : resources) {
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
				configClass.addImportedResource(resolvedResource, readerClass);
			}
		}

		// Process individual @Bean methods
        // 处理@Bean注解：获取被@Bean注解修饰的方法，然后添加到配置类的beanMethods属性中
		Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
		for (MethodMetadata methodMetadata : beanMethods) {
			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
		}

		// Process default methods on interfaces
        // 处理配置类实现的接口的默认方法,然后把默认方法加入到配置类的beanMethods属性中
		processInterfaces(configClass, sourceClass);

		// Process superclass, if any
        // 处理配置类的子类，将子类加入到配置类的knownSuperclasses属性中
		if (sourceClass.getMetadata().hasSuperClass()) {
			String superclass = sourceClass.getMetadata().getSuperClassName();
			if (superclass != null && !superclass.startsWith("java") &&
					!this.knownSuperclasses.containsKey(superclass)) {
				this.knownSuperclasses.put(superclass, configClass);
				// Superclass found, return its annotation metadata and recurse
				return sourceClass.getSuperClass();
			}
		}

		// No superclass -> processing is complete
		return null;
	}
```



1.处理内部类,这种情况比较少

2.处理@PropertySource注解

3.处理@ComponentScan注解,Spring根据@ComponentScan注解去扫描所有加了@Component注解的类，然后把这些类注册到Spring中的容器中来，我后面会来分析具体代码

4.处理@Import注解，分3种情况处理，我后面会来分析具体代码 

​	4.1 ImportSelector

​	4.2 import普通类 即import(xxx.class)以及ImportSelector返回的普通类

​	源码说的是如果候选类不是ImportSelector或ImportBeanDefinitionRegistrar->将其作为@Configuration类处	理

​	4.3 ImportBeanDefinitionRegistrar

5.处理@ImportResource注解

6.处理配置类中加了@Bean的方法，通过ASM获取被@Bean注解修饰的方法，然后添加到配置类的beanMethods属性中，具体看我下面的分析

7.处理配置类实现接口的默认方法

8.处理配置类的子类



对于以上的8个步骤，除了处理@ComponentScan注解和处理@Import注解，其他都很简单，

接下来就重点分析Spring是如何处理这两个注解的



======================================我是分隔符(*^__^*) ===================================



######5.5.5.3.1 详解解析配置类之处理@ComponentScan注解

这一步是Spring根据@ComponentScan注解去扫描所有加了@Component注解的类，然后把这些类注册到Spring中的容器中来



1.扫描前准备，解析扫描的一些基本信息，比如是否过滤，是否懒加载，比如是否加入新的包，排除不要扫描的包，然后调用doScan()方法进行真正的扫描与注册



这里要说明一点这个判断是否懒加载是通过lazyInit的值，如果lazyInit属性配置为true，那么就会进行懒加载，这样在容器初始化的过程中不会进行依赖注入，只有当第一个getBean的时候才会实例化Bean。

但是这里**仅仅只是赋值给了一个变量，还没有真正赋值给BeanDefinition**，赋值是在后面执行扫描时，创建出来的BeanDefinition就会根据这个变量来设置是否懒加载，但是如果其中一个类还是自己单独设置了@Lazy注解呢?

那么Spring其实还是会来循环判断这些被加了注解的类，来处理他们的自己的通用注解。

```java
/**
	 * 解析扫描的一些基本信息 比如是否过滤，比如是否加入新的包
	 */
public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, final String declaringClass) {
		// 这个scanner是Spring来扫描包的
    	ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,
				componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);

		// BeanNameGenerator 判断有没有外部生成器
		Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
		boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);
		scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator :
				BeanUtils.instantiateClass(generatorClass));

		// 这个要结合web，代理模型
		ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");
		if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
			scanner.setScopedProxyMode(scopedProxyMode);
		}
		else {
			Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
			scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));
		}

		scanner.setResourcePattern(componentScan.getString("resourcePattern"));

		// 遍历当中的过滤
		for (AnnotationAttributes filter : componentScan.getAnnotationArray("includeFilters")) {
			for (TypeFilter typeFilter : typeFiltersFor(filter)) {
				scanner.addIncludeFilter(typeFilter);
			}
		}
		for (AnnotationAttributes filter : componentScan.getAnnotationArray("excludeFilters")) {
			for (TypeFilter typeFilter : typeFiltersFor(filter)) {
				scanner.addExcludeFilter(typeFilter);
			}
		}

		// 根据lazyInit的值判断是否是懒加载 默认false
		boolean lazyInit = componentScan.getBoolean("lazyInit");
		if (lazyInit) {
			scanner.getBeanDefinitionDefaults().setLazyInit(true);
		}

		Set<String> basePackages = new LinkedHashSet<>();
		String[] basePackagesArray = componentScan.getStringArray("basePackages");
		for (String pkg : basePackagesArray) {
			String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),
					ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			Collections.addAll(basePackages, tokenized);
		}
		for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}
		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(declaringClass));
		}
		// 拿到配置的Exclude 排除不要扫描的包
		scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
			@Override
			protected boolean matchClassName(String className) {
				return declaringClass.equals(className);
			}
		});
		return scanner.doScan(StringUtils.toStringArray(basePackages));
	}
```



**2.通过ASM执行扫描，并向容器注册扫描到的bean**

```java
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		// 创建一个集合，存放扫描到Bean定义的封装类
		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
		// 遍历扫描所有给定的包
		for (String basePackage : basePackages) {
			// 调用父类ClassPathScanningCandidateComponentProvider的方法
			// 扫描basePackage路径下的java文件
			// 符合条件的并把它转成BeanDefinition类型
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);

			// 遍历扫描到的Bean
			for (BeanDefinition candidate : candidates) {
				// 获取Bean定义类中@Scope注解的值，即获取Bean的作用域
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				// 为Bean设置注解配置的作用域
				candidate.setScope(scopeMetadata.getScopeName());
				// 为Bean生成名称
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
				if (candidate instanceof AbstractBeanDefinition) {
					// 如果这个类是AbstractBeanDefinition的子类
					// 则为Bean设置默认值，比如lazy，init destory
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}
				// 在上面加了默认值后，再来判断如果扫描到的Bean是Spring的注解Bean，则处理其通用的Spring注解
				if (candidate instanceof AnnotatedBeanDefinition) {
					// 检查并且处理常用的注解
					// 这里的处理主要是指把常用注解的值设置到AnnotatedBeanDefinition当中
					// 当前前提是这个类必须是AnnotatedBeanDefinition类型的，说白了就是加了注解的类
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}
				// 根据Bean名称检查指定的Bean是否需要在容器中注册，或者在容器中冲突
				if (checkCandidate(beanName, candidate)) {
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
					// 根据注解中配置的作用域，为Bean应用相应的代理模式
					definitionHolder =
							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
					beanDefinitions.add(definitionHolder);
					// 向容器注册扫描到的Bean 注册到工厂里的beanDefinition map当中
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}
		return beanDefinitions;
	}
```

1.遍历扫描所有给定的包,调用父类ClassPathScanningCandidateComponentProvider的方法，通过ASM扫描basePackage路径下的class文件，符合条件的并把它转成BeanDefinition类型

2.遍历扫描到的Bean

3.获取BeanDefinition中@Scope注解的值，即获取Bean的作用域

4.为Bean设置注解配置的作用域

5.为Bean生成名称

6.如果这个类是AbstractBeanDefinition的子类，为Bean设置默认值，比如lazy,init,destory，这里肯定是其子类

7.在上面加了默认值后，再来判断如果扫描到的Bean是被Spring的注解Bean，则处理其通用的Spring注解，这个过程前面也分析过了 

8.根据Bean名称检查指定的Bean是否需要在容器中注册，或者在容器中冲突

9.根据注解中配置的作用域，为Bean应用相应的代理模式

10.向容器注册扫描到的Bean 注册到工厂里的beanDefinition map当中

​	注册过程前边都以及分析过了。



到此Spring处理@ComponectScan注解的过程分析完成。





======================================我是分隔符(*^__^*) ===================================



######5.5.5.3.2 详解解析配置类之处理@Import注解

首先处理@Import分3中情况去处理，

1.ImportSelector

2.import普通类 即import(xxx.class)，或者是ImportSelector返回的普通类

​	源码注释说的是候选类不是ImportSelector或ImportBeanDefinitionRegistrar->将其作为@Configuration类处理

3.ImportBeanDefinitionRegistrar

**和@Component不同的是解析到的beanDefinition先暂时放到了一个map中，等全部解析完后在把beanDefinition注册到Spring的beanDefinition map容器中**



先小总结一下:

这里处理的import是需要判断我们的类当中时候有@Import注解，

如果有这把@Import当中的值拿出来，这个值是一个类
比如@Import(Xxxxx.class)，那么这里便把Xxxxx传进去进行解析
在解析的过程中如果发觉是一个importSelector那么就回调selector的方法
返回一个字符串（类名），通过这个字符串得到一个类
然后再递归调用本方法来处理这个类

为什么要单独写这么多注释来说说明这个类?
因为selector返回的那个类，严格意义上来讲不符合@Import(xxxx.class)，因为这个类没有被直接import
如果不符合，就不会调用这个getImports(sourceClass)方法，即得到所有的import的类
但是注意的是递归当中是没有getImports(sourceClass)的，意思是直接把selector当中返回的类直接当成一个import的类去解析

总之就是一句话，@Import(xxx.class)，那么这个类会被解析
如果xxx是selector，那么它当中返回的类虽然没有直接加上@Import，但是也会直接解析

或者这么看 第一次来解析@Import注解的时候，通过getImports(sourceClass)来获取@Import(xxx.class)里的类，进去后会有递归调用，递归的时候是不会调用getImports(sourceClass)的，只是来把你传进来的类来判断是import的3种情况的哪一种，然后进行处理，**这时这些bean会暂时放到configurationClasses这个集合中，等全部解析完后再注册到beanDefinition map中**



我们下面跟进解析方法processImports()来分析下===>

```java
private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
			Collection<SourceClass> importCandidates, boolean checkForCircularImports) {

    	// 如果没有被Improt的类，则直接return
		if (importCandidates.isEmpty()) {
			return;
		}

    	// 如果循环Import，则抛出异常
		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
		}
		else {
            // 进栈 用于处理循环Import的情况
			this.importStack.push(configClass);
			try {
				for (SourceClass candidate : importCandidates) {
					// 判断是否是@Import(ImportSelector)
					if (candidate.isAssignable(ImportSelector.class)) {
						// Candidate class is an ImportSelector -> delegate to it to determine imports
						// 先将实现了ImportSelector的类load进来
						Class<?> candidateClass = candidate.loadClass();
						// 然后反射实现一个对象
						ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
						// 检察load进来的selector是否实现了xxxAware接口，执行这些方法
						ParserStrategyUtils.invokeAwareMethods(
								selector, this.environment, this.resourceLoader, this.registry);
												// 延迟加载, 如果selector实现了DeferredImportSelector接口，就等配置类解析完后再处理这些selector
						if (this.deferredImportSelectors != null && selector instanceof DeferredImportSelector) {
							this.deferredImportSelectors.add(
									new DeferredImportSelectorHolder(configClass, (DeferredImportSelector) selector));
						}
						else {
							// 回调
							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
							// 递归，这里第二次调用processImports
							// 如果是一个普通类，会进else
							processImports(configClass, currentSourceClass, importSourceClasses, false);
						}
					}
					// 判断是否是@Import(ImportBeanDefinitionRegistrar)
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// Candidate class is an ImportBeanDefinitionRegistrar ->
						// delegate to it to register additional bean definitions
						Class<?> candidateClass = candidate.loadClass();
						ImportBeanDefinitionRegistrar registrar =
								BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
						ParserStrategyUtils.invokeAwareMethods(
								registrar, this.environment, this.resourceLoader, this.registry);
						// 添加到一个list当中和importselector不同
						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
					}
					// 普通处理@Import("spring.xml")
					else {
						// Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
						// process it as an @Configuration class
						// 候选类不是ImportSelector或ImportBeanDefinitionRegistrar->将其作为@Configuration类处理
						// 压入importStack
						this.importStack.registerImport(
								currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
						// processConfigurationClass里面主要就是把类放到configurationClasses
						// configurationClasses是一个集合，会在后面拿出来解析成bd继而注册
						// 可以看到普通类在扫描出来的时候就被注册了
						// 如果是importSelector，会先放到configurationClasses后面进行出来注册
						processConfigurationClass(candidate.asConfigClass(configClass));
					}
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to process import candidates for configuration class [" +
						configClass.getMetadata().getClassName() + "]", ex);
			}
			finally {
                // 解析完后出栈
				this.importStack.pop();
			}
		}
	}
```

首先整个方法就是处理循环Import + 一个大循环里分成了3个if else的代码块，循环的就是配置类里@Import注解里的类，

然后第一个if判断是否是ImportSelector，第二个if判断是否是ImportSelector，最后else就直接进行处理(即是一个普通类的情况)



**处理循环Import:**

为了处理循环Import的情况，在每次进来的时候Spring会把当前的ConfigurationClass压入importStack这个栈中，等解析完后在finally代码块里再弹出来，那么Spring在递归调用这个方法的时候会将当前解析的ConfigurationClass从栈里去找以此判断是否是循环Import，如果是就抛出异常。



**解析Import的过程 我下面就按照上述3种情况来进行分析:**

一.最后else处理普通类 (候选类不是ImportSelector或ImportBeanDefinitionRegistrar->将其作为@Configuration类处理)

这种情况最简单，先把当前ConfigurationClass压入栈，然后调用processConfigurationClass()方法，这个方法我们在上面已经分析过了，

最后就是给放进了一个map中，这个map就是用来 存放解析配置类时扫描出来的bean的信息，等全部解析完配置类后再回到之前的parse()方法中，通过这个map进行后面的处理

![1584637546196](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584637546196.png)

![1584639236715](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584639236715.png)



二.如果是ImportBeanDefinitionRegistrar类型的

1.先将实现了ImportBeanDefinitionRegistrar的类load进来

2.然后反射实现一个对象

3.检查load进来的selector是否实现了xxxAware接口，执行这些方法

4.添加到一个list当中和importselector不同

![1584639142763](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584639142763.png)



三.如果是ImportSelector类型的

1.先将实现了ImportSelector的类load进来

2.然后反射实现一个对象

3.检查load进来的selector是否实现了xxxAware接口，执行这些方法

4.判断是否需要延迟加载(实现了DeferredImportSelector接口)，如果是延迟加载的就放到解析器里的一个list中，等配置类解析完后，再来解析这个list中的importSelector，至于为什么要实现DeferredImportSelector接口，就是希望等配置类全部解析完后，再来解析我这些延迟加载的importSelector

```
@Nullableprivate List<DeferredImportSelectorHolder> deferredImportSelectors;
```

5.如果不是延迟加载，就回调实现了ImportSelector的类selectImports()方法

6.selectImports方法会返回一个字符串数组，里面放的是需要import的类的名称

7.递归调用processImports()，处理被Improt的类

​	这里递归处理时，被Import的类可能也会是ImportSelector类型的，Spring也会来进行处理，

但是此时要注意这里并没有去调用getImports(sourceClass)方法，说明这里递归的时候是不会去解析@Import的，也就是说这里在处理被Improt的类上如果加了@Import注解的话，Spring是不会去解析的

​	至于递归处理时产生循环Import的情况Spring会做处理。

​	7.1 递归进来后，循环被Improt的类，如果是一个普通类就直接走else处理普通类

​	7.2 如果是ImportSelector则重复步骤1-7，直至处理完成

​	7.3 如果是ImportBeanDefinitionRegistrar 就走我上面分析ImportBeanDefinitionRegistrar 的逻辑

![1584637968119](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584637968119.png)





======================================我是分隔符(*^__^*) ===================================



######5.5.5.3.3 处理延迟加载的DeferredImportSelector

在Spring解析配置类的时候，在解析@Import注解是，如果是一个ImportSelector会去判断是否实现了DeferredImportSelector接口，如果是那么会将这些类放到一个deferredImportSelectors的list中。

这个DeferredImportSelector的作用主要是在SpringBoot里的自动配置里体现的

![1584678939165](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584678939165.png)





下面我们先回到之前的调用链里 即5.5.5.3 解析配置文件 parser.parse(candidates); 这段逻辑

![1584679047227](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584679047227.png)



下面我们点进processDeferredImportSelectors();方法里，实际上我们看到他这里最终还是调用了processImports()方法来进行解析，逻辑就是我们上面所分析的

```java
private void processDeferredImportSelectors() {
		List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
		this.deferredImportSelectors = null;
		if (deferredImports == null) {
			return;
		}

		// 对deferredImports排序
		deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);
		Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();
		Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();
		// 迭代deferredImport
		for (DeferredImportSelectorHolder deferredImport : deferredImports) {
			//  通过ImportSelector的ImportGroup进行分组
			Class<? extends Group> group = deferredImport.getImportSelector().getImportGroup();
			DeferredImportSelectorGrouping grouping = groupings.computeIfAbsent(
					(group != null ? group : deferredImport),
					key -> new DeferredImportSelectorGrouping(createGroup(group)));
			grouping.add(deferredImport);
			// 设置引入该import的configurationClass  例如AutoConfigurationImportSelector则为主启动类
			configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(),
					deferredImport.getConfigurationClass());
		}
		for (DeferredImportSelectorGrouping grouping : groupings.values()) {
			grouping.getImports().forEach(entry -> {
				// 获取到引入import的configurationClass
				ConfigurationClass configurationClass = configurationClasses.get(entry.getMetadata());
				try {
					// 进行解析这些importSelector
					processImports(configurationClass, asSourceClass(configurationClass),
							asSourceClasses(entry.getImportClassName()), false);
				}
				catch (BeanDefinitionStoreException ex) {
					throw ex;
				}
				catch (Throwable ex) {
					throw new BeanDefinitionStoreException(
							"Failed to process import candidates for configuration class [" +
							configurationClass.getMetadata().getClassName() + "]", ex);
				}
			});
		}
	}
```





======================================我是分隔符(*^__^*) ===================================



######5.5.5.3.4 详解解析配置类之处理@Bean注解

对于@Bean标注的方法，在解析配置类的过程中获取被@Bean注解修饰的方法，然后添加到配置类的beanMethods属性中

![1584682951705](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584682951705.png)



下面我来分析下Spring是如何解析被标注@Bean注解的方法的,我们点进retrieveBeanMethodMetadata(sourceClass)方法

```java
/**
	 * Retrieve the metadata for all <code>@Bean</code> methods.
	 *
	 * 解析配置类里的@Bean方法
	 */
	private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
		// 获取类的元数据
		AnnotationMetadata original = sourceClass.getMetadata();
		// 从元数据内获取注解有@Bean的方法集合
		Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
		// 判断被标注@Bean的方法是否超过1个，并且类的元数据是StandardAnnotationMetadata实例
		// 则通过ASM读取类文件以获得确定的声明顺序
		// 因为，JVM的标准反射库获取的方法顺序是随机的，即使同一个程序在相同的JVM上运行上运行结果也是不同的(随机的)
		if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
			// Try reading the class file via ASM for deterministic declaration order...
			// Unfortunately, the JVM's standard reflection returns methods in arbitrary
			// order, even between different runs of the same application on the same JVM.
			try {
				AnnotationMetadata asm =
						this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
				if (asmMethods.size() >= beanMethods.size()) {
					Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
					// 做了一遍过滤，将二进制class文件的方法与JVM反射的方法做一次对比，
					// 防止将非编程方法加入，保持与JVM反射获取方法一致，除了顺序。
					for (MethodMetadata asmMethod : asmMethods) {
						for (MethodMetadata beanMethod : beanMethods) {
							if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
								selectedMethods.add(beanMethod);
								break;
							}
						}
					}
					if (selectedMethods.size() == beanMethods.size()) {
						// All reflection-detected methods found in ASM method set -> proceed
						beanMethods = selectedMethods;
					}
				}
			}
			catch (IOException ex) {
				logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
				// No worries, let's continue with the reflection metadata we started with...
			}
		}
		// 返回被解析到的方法，如果是被标注的方法没有超过1个，没有进if就直接返回
		return beanMethods;
	}
```

1.获取类的元数据

2.从元数据内获取注解有@Bean的方法集合

3.判断被标注@Bean的方法是否超过1个，并且类的元数据是StandardAnnotationMetadata实例，则通过ASM来获取。

​	至于为什么通过ASM来获取 ，源码里的注释写明白了

Try reading the class file via ASM for deterministic declaration order...Unfortunately, the JVM's standard reflection returns methods in arbitraryorder, even between different runs of the same application on the same JVM.

因为，JVM的标准反射库获取的方法顺序是随机的，即使同一个程序在相同的JVM上运行上运行结果也是不同的(随机的)



4.返回被解析到的方法，如果是被标注的方法没有超过1个，没有进if就直接返回





======================================我是分隔符(*^__^*) ===================================



至此解析配置类的过程就结束了，接下来就是要将那些在解析过程中被放入configurationClasses这个map中的ConfigurationClass注册到Spring的beanDefinition map中



接下来我们回到ConfigurationClassPostProcessor类中的processConfigBeanDefinitions(BeanDefinitionRegistry registry)方法，也就是5.5.5

======================================我是分隔符(*^__^*) ===================================



#####5.5.5.4  扫描完配置类后 读取除了加了@Component的类以外的所有ConfigurationClass，将这些ConfigurationClass注册到bean工厂的beanDefinition map中



我们回到ConfigurationClassPostProcessor类中的processConfigBeanDefinitions(BeanDefinitionRegistry registry)方法后，当解析配置类完成后，调用了下面方法来进行注册

```
this.reader.loadBeanDefinitions(configClasses);
```

点进去看是个套娃方法，循环每个ConfigurationClass，调用loadBeanDefinitionsForConfigurationClass()方法处理

```
public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
		TrackedConditionEvaluator trackedConditionEvaluator = new TrackedConditionEvaluator();
		for (ConfigurationClass configClass : configurationModel) {
			loadBeanDefinitionsForConfigurationClass(configClass, trackedConditionEvaluator);
		}
	}
```



点进loadBeanDefinitionsForConfigurationClass===>我们看到根据不同的ConfigurationClass,这里委派了4种不同的方法去进行注册

```java
/**
	 * Read a particular {@link ConfigurationClass}, registering bean definitions
	 * for the class itself and all of its {@link Bean} methods.
	 */
	private void loadBeanDefinitionsForConfigurationClass(
			ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {

		if (trackedConditionEvaluator.shouldSkip(configClass)) {
			String beanName = configClass.getBeanName();
			if (StringUtils.hasLength(beanName) && this.registry.containsBeanDefinition(beanName)) {
				this.registry.removeBeanDefinition(beanName);
			}
			this.importRegistry.removeImportingClass(configClass.getMetadata().getClassName());
			return;
		}

		// 如果一个类是被import的，会被spring标注
		// 在这里完成注册
		if (configClass.isImported()) {
			registerBeanDefinitionForImportedConfigurationClass(configClass);
		}
		// @Bean
		for (BeanMethod beanMethod : configClass.getBeanMethods()) {
			loadBeanDefinitionsForBeanMethod(beanMethod);
		}

		// @ImportedResources导入的xml
		loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
		// 执行Registrar
	loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());
	}
```





======================================我是分隔符(*^__^*) ===================================



######5.5.5.4.1 注册@Import进来的类

点进registerBeanDefinitionForImportedConfigurationClass(configClass);分析

```java
private void registerBeanDefinitionForImportedConfigurationClass(ConfigurationClass configClass) {
		// 获取元数据
		AnnotationMetadata metadata = configClass.getMetadata();
		// 封装成一个AnnotatedGenericBeanDefinition对象
		AnnotatedGenericBeanDefinition configBeanDef = new AnnotatedGenericBeanDefinition(metadata);

		// 获取并设置代理模型
		ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(configBeanDef);
		configBeanDef.setScope(scopeMetadata.getScopeName());
		// 生成beanName
		String configBeanName = this.importBeanNameGenerator.generateBeanName(configBeanDef, this.registry);
		// 处理通用注解@Lazy，@Primary，@DependsOn
		AnnotationConfigUtils.processCommonDefinitionAnnotations(configBeanDef, metadata);

		// 封装成BeanDefinitionHolder类型
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(configBeanDef, configBeanName);
		// 为注册的Bean创建相应模式的代理对象
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		// 注册bean
		this.registry.registerBeanDefinition(definitionHolder.getBeanName(), definitionHolder.getBeanDefinition());
		configClass.setBeanName(configBeanName);

		if (logger.isDebugEnabled()) {
			logger.debug("Registered bean definition for imported class '" + configBeanName + "'");
		}
	}
```



这里的逻辑就很简单了

1.获取元数据然后封装成一个AnnotatedGenericBeanDefinition对象

2.获取并设置代理模型

3.生成beanName

4.处理通用注解@Lazy，@Primary，@DependsOn

5.注册bean



======================================我是分隔符(*^__^*) ===================================



######5.5.5.4.2 注册@Bean

```java
private void loadBeanDefinitionsForBeanMethod(BeanMethod beanMethod) {
		// 获取拥有这个beanMethod的配置类
		ConfigurationClass configClass = beanMethod.getConfigurationClass();
		// 获取beanMethod的methodName
		MethodMetadata metadata = beanMethod.getMetadata();
		String methodName = metadata.getMethodName();

		// Do we need to mark the bean as skipped by its condition?
		// 处理@Conditional注解以判断是否需要跳过
		if (this.conditionEvaluator.shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN)) {
			configClass.skippedBeanMethods.add(methodName);
			return;
		}
		if (configClass.skippedBeanMethods.contains(methodName)) {
			return;
		}

		// 从@Bean中获得配置的names,如果names不为空的话,则第一个为bean的id,否则该方法名字作为bean的id
		AnnotationAttributes bean = AnnotationConfigUtils.attributesFor(metadata, Bean.class);
		Assert.state(bean != null, "No @Bean annotation attributes");

		// Consider name and any aliases
		List<String> names = new ArrayList<>(Arrays.asList(bean.getStringArray("name")));
		String beanName = (!names.isEmpty() ? names.remove(0) : methodName);

		// Register aliases even when overridden
		// 根据从@Bean中获得配置的name注解别名
		for (String alias : names) {
			this.registry.registerAlias(beanName, alias);
		}

		// Has this effectively been overridden before (e.g. via XML)?
		// 判断是否存在重复定义（例如通过XML）
		if (isOverriddenByExistingDefinition(beanMethod, beanName)) {
			if (beanName.equals(beanMethod.getConfigurationClass().getBeanName())) {
				throw new BeanDefinitionStoreException(beanMethod.getConfigurationClass().getResource().getDescription(),
						beanName, "Bean name derived from @Bean method '" + beanMethod.getMetadata().getMethodName() +
						"' clashes with bean name for containing configuration class; please make those names unique!");
			}
			return;
		}

		// 将配置类包装成ConfigurationClassBeanDefinition
		ConfigurationClassBeanDefinition beanDef = new ConfigurationClassBeanDefinition(configClass, metadata);
		beanDef.setResource(configClass.getResource());
		beanDef.setSource(this.sourceExtractor.extractSource(metadata, configClass.getResource()));

		// 如果该方法是静态的,则将methodName设置为工厂方法
		if (metadata.isStatic()) {
			// static @Bean method
			beanDef.setBeanClassName(configClass.getMetadata().getClassName());
			beanDef.setFactoryMethodName(methodName);
		}
		else {
			// instance @Bean method
			// 如果是实例方法的话,则将configClass的BeanName设置为FactoryBeanName,methodName设置为UniqueFactoryMethodName
			beanDef.setFactoryBeanName(configClass.getBeanName());
			beanDef.setUniqueFactoryMethodName(methodName);
		}
		// 设置AutowireMode为构造器注入
		beanDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		// 设置skipRequiredCheck属性为true.
		beanDef.setAttribute(RequiredAnnotationBeanPostProcessor.SKIP_REQUIRED_CHECK_ATTRIBUTE, Boolean.TRUE);

		// 处理通用注解@Lazy，@Primary，@DependsOn
		AnnotationConfigUtils.processCommonDefinitionAnnotations(beanDef, metadata);

		Autowire autowire = bean.getEnum("autowire");
		if (autowire.isAutowire()) {
			beanDef.setAutowireMode(autowire.value());
		}

		String initMethodName = bean.getString("initMethod");
		// 设置 InitMethod
		if (StringUtils.hasText(initMethodName)) {
			beanDef.setInitMethodName(initMethodName);
		}

		// 设置 DestroyMethod
		String destroyMethodName = bean.getString("destroyMethod");
		beanDef.setDestroyMethodName(destroyMethodName);

		// Consider scoping
		// 设置代理模型
		ScopedProxyMode proxyMode = ScopedProxyMode.NO;
		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(metadata, Scope.class);
		if (attributes != null) {
			beanDef.setScope(attributes.getString("value"));
			proxyMode = attributes.getEnum("proxyMode");
			if (proxyMode == ScopedProxyMode.DEFAULT) {
				proxyMode = ScopedProxyMode.NO;
			}
		}

		// Replace the original bean definition with the target one, if necessary
		// 如果ScopedProxyMode 不等于NO,则生成代理
		BeanDefinition beanDefToRegister = beanDef;
		if (proxyMode != ScopedProxyMode.NO) {
			BeanDefinitionHolder proxyDef = ScopedProxyCreator.createScopedProxy(
					new BeanDefinitionHolder(beanDef, beanName), this.registry,
					proxyMode == ScopedProxyMode.TARGET_CLASS);
			beanDefToRegister = new ConfigurationClassBeanDefinition(
					(RootBeanDefinition) proxyDef.getBeanDefinition(), configClass, metadata);
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Registering bean definition for @Bean method %s.%s()",
					configClass.getMetadata().getClassName(), beanName));
		}
		// 注册bean
		this.registry.registerBeanDefinition(beanName, beanDefToRegister);
	}
```

其中过程比较多，但大体都是类似的，主要就是最后调用了registry对象进行注册，

不过这个过程中要注意的是static修饰的，是通过工厂方法来创建对象的





######5.5.5.4.3 执行ImportBeanDefinitionRegistrar

点进loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());

```java
private void loadBeanDefinitionsFromRegistrars(Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> registrars) {
		registrars.forEach((registrar, metadata) ->
				registrar.registerBeanDefinitions(metadata, this.registry));
	}
```

这里就很简单，遍历之前解析到的ImportBeanDefinitionRegistrar，然后执行其registerBeanDefinitions()方法





======================================我是分隔符(*^__^*) ===================================



到此ConfigurationClassPostProcessor类的postProcessBeanDefinitionRegistry()方法执行逻辑已经结束，

这个回调的方法就是来解析我们的配置类。

那么由于ConfigurationClassPostProcessor是实现了BeanDefinitionRegistryPostProcessor接口，那么实际上再执行完postProcessBeanDefinitionRegistry()的回调后，还会执行父接口里postProcessBeanFactory()的回调，

接下来我就来分析这个postProcessBeanFactory()的回调，这个也非常重要



======================================我是分隔符(*^__^*) ===================================





####5.5.6  ConfigurationClassPostProcessor类的postProcessBeanFactory() 方法逻辑分析



```java
/**
	 * Prepare the Configuration classes for servicing bean requests at runtime
	 * by replacing them with CGLIB-enhanced subclasses.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		int factoryId = System.identityHashCode(beanFactory);
		if (this.factoriesPostProcessed.contains(factoryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + beanFactory);
		}
		this.factoriesPostProcessed.add(factoryId);
		if (!this.registriesPostProcessed.contains(factoryId)) {
			// BeanDefinitionRegistryPostProcessor hook apparently not supported...
			// Simply call processConfigurationClasses lazily at this point then.
			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		}

		// 给全配置类产生cglib代理
		// 为什么需要产生cglib代理？
		enhanceConfigurationClasses(beanFactory);
        // 给bean工厂增加一个ImportAwareBeanPostProcessor后置处理器，用于给代理类注入beanFactory
		beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
	}
```

1.调用enhanceConfigurationClasses(beanFactory)方法给全配置类产生cglib代理

2.给bean工厂增加一个ImportAwareBeanPostProcessor后置处理器，用于给cglib代理过后的配置类所生成的代理类注入一个beanFactory，还用于处理ImportAware，具体看5.5.6.7





======================================我是分隔符(*^__^*) ===================================



#####5.5.6.1 Spring配置类的Full和Lite到底是什么意思? 即加了@Configuration和不加@Configuration到底什么区别

在前面我已经分析过，在Spring解析配置类的时候如果一个类加了@Configuration就会被标注成Full，没有加@Configuration但加了@Component，@ComponentScan，@Import，@ImportResource这几个注解的，或者类中有方法标注了@Bean，就会被标注成Lite，而且我们知道不管是Full和Lite的配置类Spring都会去对其做解析，那么区别到底是什么呢？



区别就在于，如果 加了@Configuration，Spring就会给该配置类生成cglib代理，最终会通过该代理类来拦截目标对象中的@Bean方法，去判断代理方法的名字和参数是否 和 正在执行的方法一样，如果一样则通过目表方法来获取bean，否则通过beanFactory来获取bean。



######5.5.6.1.1 演示配置类加了@Configuration和不加@Configuration的区别

下面我通过一个例子来演示下

1.配置类  注意orderService()里调用了userService()

```java
@Configuration
public class SpringConfig {

	@Bean
	public UserService userService() {
		return new UserService();
	}

	@Bean
	public OrderService orderService() {
        // 注意我这里调用了userService()
		userService();
		return new OrderService();
	}

}
```



2.UserService  通过构造方法打印信息，看UserService被实例化了几次

```java
public class UserService {
    
	public UserService() {
		System.out.println("userService init..");
	}

	public void query() {
		System.out.println("userService query..");
	}

}
```



3.OrderService

```java
public class OrderService {
	public void query() {
		System.out.println("orderService query..");
	}
}
```



4.测试类

```java
public class TestCglib {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(SpringConfig.class);

	}
}
```



配置类加了@Configuration的测试结果，可以看到UserService只实例化了一次

![1584802260909](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584802260909.png)





配置类不加@Configuration的测试结果，可以看到UserService实例化了两次

![1584802370306](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584802370306.png)





======================================我是分隔符(*^__^*) ===================================



#####5.5.6.2 判断配置类是否要进行cglib代理过程

enhanceConfigurationClasses方法分析===> **先总结下就是遍历已经注册的bean，如果加了@Configuration的类就进行cglib代理**

```java
/**
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
	 * Candidate status is determined by BeanDefinition attribute metadata.
	 * @see ConfigurationClassEnhancer
	 */
	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			// 判断是否是一个全注解类
			// 扫描是全注解类？full和lite的关系
			if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {
				if (!(beanDef instanceof AbstractBeanDefinition)) {
					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
				}
				else if (logger.isWarnEnabled() && beanFactory.containsSingleton(beanName)) {
					logger.warn("Cannot enhance @Configuration bean definition '" + beanName +
							"' since its singleton instance has been created too early. The typical cause " +
							"is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
							"return type: Consider declaring such methods as 'static'.");
				}
				configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
			}
		}
		if (configBeanDefs.isEmpty()) {
			// nothing to enhance -> return immediately
			return;
		}

		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			AbstractBeanDefinition beanDef = entry.getValue();
			// If a @Configuration class gets proxied, always proxy the target class
			beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			try {
				// Set enhanced subclass of the user-specified bean class
				Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
				if (configClass != null) {
					// 完成对全注解类的cglib代理
					Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
					if (configClass != enhancedClass) {
						if (logger.isDebugEnabled()) {
							logger.debug(String.format("Replacing bean definition '%s' existing class '%s' with " +
									"enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
						}
						beanDef.setBeanClass(enhancedClass);
					}
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
			}
		}
	}

```

1.循环beanFactory里的getBeanDefinitionNames，根据beanName取出beanDefinition

2.根据beanDefinition判断该类是否是一个被标注成Full的类(也就是加了@Configuration的类)，然后放到一个LinkedHashMap

3.如果该map为空就直接返回

4.如果map不为空，就遍历map，取出beanDefinition里的beanClass，对该beanClass进行cglib代理

```java
// 完成对Full注解类的cglib代理
Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
```





下面我们点进进行cglib代理的方法===>

```java
/**
	 * Loads the specified class and generates a CGLIB subclass of it equipped with
	 * container-aware callbacks capable of respecting scoping and other bean semantics.
	 * @return the enhanced subclass
	 */
	public Class<?> enhance(Class<?> configClass, @Nullable ClassLoader classLoader) {
		// 判断是否已经是被cglib代理过后生成的代理类, 如何判断?
		// EnhancedConfiguration接口是Spring内部判断一个配置类是否被cglib代理过，代理过的会实现这个接口
		if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Ignoring request to enhance %s as it has " +
						"already been enhanced. This usually indicates that more than one " +
						"ConfigurationClassPostProcessor has been registered (e.g. via " +
						"<context:annotation-config>). This is harmless, but you may " +
						"want check your configuration and remove one CCPP if possible",
						configClass.getName()));
			}
			return configClass;
		}
		// 没有被代理cglib代理
		Class<?> enhancedClass = createClass(newEnhancer(configClass, classLoader));
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Successfully enhanced %s; enhanced class name is: %s",
					configClass.getName(), enhancedClass.getName()));
		}
		return enhancedClass;
	}
```

1. 先判断是否已经被cglib代理过，如果被代理过就return

   如何判断? 看5.5.6.2

   

2. 如果没有被代理过就执行代理，最后返回代理类

   

   

======================================我是分隔符(*^__^*) ===================================





#####5.5.6.3 Spring内部或第三方如何判断一个配置类是否已经被Spring进行了cglib代理

当Spring内部在对一个配置类进行cglib代理的时候会让代理类去实现一个Spring的内部接口EnhancedConfiguration，如果实现过这个接口，说明传入的这个类已经是被cglib代理过的类。

同时基于该接口的父接口BeanFactoryAware中的setBeanFactory方法，设置此变量的值为当前Context中的beanFactory,这样一来我们这个cglib代理的对象就有了beanFactory



EnhancedConfiguration接口是Spring内部判断一个配置类是否被cglib代理过，代理过的会实现这个接口，这个接口仅仅是Spring内部提供的，但也需要提供对外第三方的公共访问权限，以让第三方可以来进行判断一个配置类是否被cglib代理过

```java
/**
	 * Marker interface to be implemented by all @Configuration CGLIB subclasses.
	 * Facilitates idempotent behavior for {@link ConfigurationClassEnhancer#enhance}
	 * through checking to see if candidate classes are already assignable to it, e.g.
	 * have already been enhanced.
	 * <p>Also extends {@link BeanFactoryAware}, as all enhanced {@code @Configuration}
	 * classes require access to the {@link BeanFactory} that created them.
	 * <p>Note that this interface is intended for framework-internal use only, however
	 * must remain public in order to allow access to subclasses generated from other
	 * packages (i.e. user code).
	 *
	 * Spring内部判断一个配置类是否被cglib代理过，代理过的会实现这个接口，
	 * 这个接口仅仅是Spring内部提供的，但也需要提供对外第三方的公共访问权限，
	 * 以让第三方可以来进行判断一个配置类是否被cglib代理过，
	 * 
	 * 同时基于该接口的父接口BeanFactoryAware中的setBeanFactory方法，
	 * 设置此变量的值为当前Context中的beanFactory,这样一来我们这个cglib代理的对象就有了beanFactory
	 */
	public interface EnhancedConfiguration extends BeanFactoryAware {
	}
```





======================================我是分隔符(*^__^*) ===================================



#####5.5.6.4 配置类进行cglib代理过程

下面点进newEnhancer(configClass, classLoader)方法分析代理过程

```java
/**
	 * Creates a new CGLIB {@link Enhancer} instance.
	 */
	private Enhancer newEnhancer(Class<?> configSuperClass, @Nullable ClassLoader classLoader) {
		Enhancer enhancer = new Enhancer();
		// 增强父类，cglib是基于继承来的
		enhancer.setSuperclass(configSuperClass);
		// 增强接口，为什么要增强接口?
		// 便于判断，表示一个类已经被Spring进行了cglib代理
		enhancer.setInterfaces(new Class<?>[] {EnhancedConfiguration.class});
		// 不继承Factory接口
		enhancer.setUseFactory(false);
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		// BeanFactoryAwareGeneratorStrategy是一个生成策略
		// 主要为生成的CGLIB类中添加成员变量$$beanFactory
		// 同时基于接口EnhancedConfiguration的父接口BeanFactoryAware中的setBeanFactory方法，
		// 设置此变量的值为当前Context中的beanFactory,这样一来我们这个cglib代理的对象就有了beanFactory
		// 有了factory就能获得对象，而不用去通过方法获得对象了，因为通过方法获得对象不能控制其过程
		// 该BeanFactory的作用是在this调用时拦截该调用，并直接在beanFactory中获得目标bean
		enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
		// 1.过滤方法，不能每次都去new
        // 2.如果配置类已经实现了BeanFactoryAware接口要注入beanFactory，那么就直接通过BeanFactoryAwareMethodInterceptor拦截器来给其注入beanFactory，如果没有，就退出
		enhancer.setCallbackFilter(CALLBACK_FILTER);
		enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
		return enhancer;
	}
```

1.进行cglib代理的操作，让代理类实现了一个EnhancedConfiguration接口，用于在Spring内部表示已结是cglib的代理类，同时基于该接口的父接口BeanFactoryAware中的setBeanFactory方法，设置此变量的值为当前Context中的beanFactory,这样一来我们这个cglib代理的对象就有了beanFactory

2.BeanFactoryAwareGeneratorStrategy是一个生成策略，主要为生成的CGLIB类中添加成员变量$$beanFactory

3.添加 了CallbackFilter

​	2.1 用于增强方法，主要控制bean的作用域

​	2.2 如果配置类已经实现了BeanFactoryAware接口要注入beanFactory，那么就直接通过BeanFactoryAwareMethodInterceptor拦截器来给其注入beanFactory，此时的话我们配置类的代理类就有了两个beanFactory，但并不冲突，我们自己的beanFactory是我们自己逻辑里使用的(比如我们写代码的时候还没有代理类)，$$beanFactory是Spring内部使用的。如果没有，就退出。

```
private static final ConditionalCallbackFilter CALLBACK_FILTER = new ConditionalCallbackFilter(CALLBACKS);
```

```java
private static final Callback[] CALLBACKS = new Callback[] {
			// 增强方法，主要控制bean的作用域
			// 不每一次都去调用new
			new BeanMethodInterceptor(),
			// 作用是判断 实际（非CGLIB）超类是否实现BeanFactoryAware？
			// 如果是，请调用其setBeanFactory（）方法
    		// 此时的话我们配置类的代理类就有了两个beanFactory，但并不冲突，我们自己的beanFactory是我们自己逻辑里使用的(比较我们写代码的时候还没有代理类)，$$beanFactory是Spring内部使用的
    		// 如果没有，就退出
			new BeanFactoryAwareMethodInterceptor(),
			NoOp.INSTANCE
	};
```





======================================我是分隔符(*^__^*) ===================================





#####5.5.6.5 cglib代理类callbacsk之BeanMethodInterceptor分析

类声明如下：

```
private static class BeanMethodInterceptor implements MethodInterceptor, ConditionalCallback {
		...........................
}
```

该类用于代理类拦截@Bean方法的调用，如果代理方法和当前执行的方法不是同一个名字且参数相同就直接从BeanFactory中获取目标bean，而不是通过执行方法





######5.5.6.5.1 BeanMethodInterceptor之intercept方法分析

```java
/**
		 * Enhance a {@link Bean @Bean} method to check the supplied BeanFactory for the
		 * existence of this bean object.
		 * @throws Throwable as a catch-all for any exception that may be thrown when invoking the
		 * super implementation of the proxied method i.e., the actual {@code @Bean} method
		 */
		@Override
		@Nullable
		public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,
					MethodProxy cglibMethodProxy) throws Throwable {

			// enhancedConfigInstance 代理对象
			// 通过enhancedConfigInstance中cglib生成的成员变量$$beanFactory获得beanFactory。
			ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);
			// 获取beanName 如果没有加beanName就通过方法名得到beanName
			String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod);

			// Determine whether this bean is a scoped-proxy
			Scope scope = AnnotatedElementUtils.findMergedAnnotation(beanMethod, Scope.class);
			if (scope != null && scope.proxyMode() != ScopedProxyMode.NO) {
				String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);
				if (beanFactory.isCurrentlyInCreation(scopedBeanName)) {
					beanName = scopedBeanName;
				}
			}

			// To handle the case of an inter-bean method reference, we must explicitly check the
			// container for already cached instances.

			// First, check to see if the requested bean is a FactoryBean. If so, create a subclass
			// proxy that intercepts calls to getObject() and returns any cached bean instance.
			// This ensures that the semantics of calling a FactoryBean from within @Bean methods
			// is the same as that of referring to a FactoryBean within XML. See SPR-6602.
            /**
			 * 为了处理bean间方法引用的情况，必须显式地检查已缓存实例的容器。
			 * 首先，检查请求的bean是否是FactoryBean。
			 * 如果是，则创建一个子类拦截对getObject（）的调用并返回任何缓存bean实例的代理。
			 * 这确保了从@Bean方法中调用FactoryBean的语义，与在XML中引用FactoryBean相同
			 */
			if (factoryContainsBean(beanFactory, BeanFactory.FACTORY_BEAN_PREFIX + beanName) &&
					factoryContainsBean(beanFactory, beanName)) {
				Object factoryBean = beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
				if (factoryBean instanceof ScopedProxyFactoryBean) {
					// Scoped proxy factory beans are a special case and should not be further proxied
				}
				else {
					// It is a candidate FactoryBean - go ahead with enhancement
					return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);
				}
			}



			// 判断代理方法的名字和参数是否和正在执行的方法一样
            // 以此判断判断执行的方法和调用方法是不是同一个方法
            // 如果为true则执行目标方法获取bean，否则从beanFactory中获取
			if (isCurrentlyInvokedFactoryMethod(beanMethod)) {
				// The factory is calling the bean method in order to instantiate and register the bean
				// (i.e. via a getBean() call) -> invoke the super implementation of the method to actually
				// create the bean instance.
				if (logger.isWarnEnabled() &&
						BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {
					logger.warn(String.format("@Bean method %s.%s is non-static and returns an object " +
									"assignable to Spring's BeanFactoryPostProcessor interface. This will " +
									"result in a failure to process annotations such as @Autowired, " +
									"@Resource and @PostConstruct within the method's declaring " +
									"@Configuration class. Add the 'static' modifier to this method to avoid " +
									"these container lifecycle issues; see @Bean javadoc for complete details.",
							beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName()));
				}
				return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
			}

			return resolveBeanReference(beanMethod, beanMethodArgs, beanFactory, beanName);
		}
```

1.通过enhancedConfigInstance中cglib生成的成员变量$$beanFactory获得beanFactory

​	enhancedConfigInstance是代理对象

2.获取beanName 如果没有加beanName就通过方法名得到beanName

3.确定此bean是否为作用域代理

4.处理FactoryBean

​	为了处理bean间方法引用的情况，必须显式地检查已缓存实例的容器。

​	首先，检查请求的bean是否是FactoryBean。如果是，则创建一个子类拦截对getObject（）的调用并返回任何	缓存bean实例的代理。

​	这确保了从@Bean方法中调用FactoryBean的语义，与在XML中引用FactoryBean相同



5.判断代理方法和当前执行的方法**同名且参数相同**

​	5.1 如果相同就执行被代理对象的方法获取bean

​	5.2 如果不相同就直接从BeanFactory中获取目标bean



======================================我是分隔符(*^__^*) ===================================



######5.5.6.5.2 对于@Bean，Spring如何判断是否要从BeanFactory中获取目标bean

```java
private boolean isCurrentlyInvokedFactoryMethod(Method method) {
			Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
			return (currentlyInvoked != null && method.getName().equals(currentlyInvoked.getName()) &&
					Arrays.equals(method.getParameterTypes(), currentlyInvoked.getParameterTypes()));
		}
```

1.将正在执行的方法的代理方法信息取出

2.判断如果有代理方法，判断代理方法的名字和参数是否和正在执行的方法一样



======================================我是分隔符(*^__^*) ===================================





#####5.5.6.6   cglib代理类callbacsk之BeanFactoryAwareMethodInterceptor分析

intercept方法分析

```java
public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			// 找到代理类里的属性 $$beanFactory
			Field field = ReflectionUtils.findField(obj.getClass(), BEAN_FACTORY_FIELD);
			Assert.state(field != null, "Unable to find generated BeanFactory field");
			field.set(obj, args[0]);

			// Does the actual (non-CGLIB) superclass implement BeanFactoryAware?
			// If so, call its setBeanFactory() method. If not, just exit.
			// 实际（非CGLIB）超类是否实现BeanFactoryAware？
			// 如果是，请调用其setBeanFactory（）方法。如果没有，就退出
			if (BeanFactoryAware.class.isAssignableFrom(ClassUtils.getUserClass(obj.getClass().getSuperclass()))) {
				return proxy.invokeSuper(obj, args);
			}
			return null;
		}

```

就是说Spring在给配置类生产代理类的时候会帮其注入一个beanFactory，但如果我们的配置类本身就是实现了

BeanFactoryAware接口要注入beanFactory，就通过这个拦截器来实现，此时的话我们配置类的代理类就有了两个beanFactory，但并不冲突，我们自己的beanFactory是我们自己逻辑里使用的(比较我们写代码的时候还没有代理类)，$$beanFactory是Spring内部使用的

![1584805196019](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584805196019.png)





#####5.5.6.7  ImportAwareBeanPostProcessor后置处理器源码分析

在  ConfigurationClassPostProcessor类的postProcessBeanFactory() 方法的最后给beanFactory加了这个一个bean后置处理器，我们先来看下代码：

```java
private static class ImportAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

		private final BeanFactory beanFactory;

		public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public PropertyValues postProcessPropertyValues(
				PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

			// Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
			// postProcessPropertyValues method attempts to autowire other configuration beans.
			/**
			 * 1.用于在AutowiredAnnotationBeanPostProcessor中的
			 * 	postProcessPropertyValues()方法尝试去自动装配其他的配置bean 之前 注入beanFactory
			 *
			 * 就是Spring在自动注入一个配置类的时候，如果这个配置类被cglib代理了，
			 * 就会实现一个EnhancedConfiguration接口 并且 代理类中会生成一个名为$$beanFactory属性，这个之前我分析过，
			 * 然后通过这个bean后置处理器来给这个cglib代理类注入beanFactory
			 */
			if (bean instanceof EnhancedConfiguration) {
				((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);
			}
			return pvs;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			// 2.处理ImportAware
			if (bean instanceof ImportAware) {
				ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
				AnnotationMetadata importingClass = ir.getImportingClassFor(bean.getClass().getSuperclass().getName());
				if (importingClass != null) {
					((ImportAware) bean).setImportMetadata(importingClass);
				}
			}
			return bean;
		}
	}
```
两个作用：看下面



######5.5.6.7.1 通过ImportAwareBeanPostProcessor给配置类cglib代理后的代理类注入beanFactory

1.如果我们配置了加了@Configuration注解就会被cglib代理，生成的代理类会实现EnhancedConfiguration接口，并生成一个名为$$beanFactory属性，然后通过这个后置处理器，再通过EnhancedConfiguration的父接口BeanFactoryAware来给代理类注入一个beanFactory

如图所示:

在执行该处理器之前，springConfigByAop是我加了@Configuration的配置类，cglib代理后的代理类里有一个$$beanFactory，值为null

![1584866091459](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584866091459.png)



然后，Spring通过ImportAwareBeanPostProcessor这个处理器的postProcessPropertyValues()方法为其$$beanFctory属性注入了beanFactory

![1584866183301](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584866183301.png)



2.处理实现了ImportAware的bean





######5.5.6.7.2 ImportAware案例演示，以及源码分析

源码：

就是当一个bean实现了ImportAware接口后，会在这里取出 通过@Import注解导入了这个类的类 的注解信息，然后调用这个bean的setImprtMetadata方法，这样就可以取到开关注解内的值

![1584867065635](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584867065635.png)



案例：

1.自定义开关注解 这个注解内加上了@Import(DBConfig.class)，将DBConfig类导入

```java
/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 2:15
 * @Version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(DBConfig.class)
public @interface EnableDBSession {

	// 模拟一个动态的开关，动态给db配置文件赋值
	String username() default "root";

}

```



2.模拟需要动态赋值的dbConfig配置类

```java
/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 2:14
 * @Version 1.0
 *
 * 通过ImportAware接口，实现一个动态开关
 */
@Configuration
public class DBConfig implements ImportAware {

	// 通过开关来动态赋值
	private String username = "test";

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableDBSession.class.getName());
		AnnotationAttributes attrs = AnnotationAttributes.fromMap(map);
		this.username = attrs.getString("username");
	}

	public String getUsername() {
		return username;
	}
}

```



3.Spring配置类，将开关注解加上后，实际上就是取这个配置类的注解信息，传给DBConfig的setImportMetadata()方法

```java
/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 1:29
 * @Version 1.0
 */
@Configuration
@ComponentScan("com.aop")
@EnableDBSession // 自己模拟的动态开关
public class SpringConfigByAop {


}

```



4.结果

Spring配置类没有加上Enable注解时，打印的是dbConfig里name的默认值

![1584865636254](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584865636254.png)



加上Enable注解后，看到dbConfig里name值被修改了

![1584866598969](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584866598969.png)







======================================我是分隔符(*^__^*) ===================================



至此invokeBeanFactoryPostProcessors(beanFactory); 在spring的环境中去执行所有注册的BeanFactoryPostProcessor的Bean 这一步分析完成



======================================我是分隔符(*^__^*) ===================================





###5.6 为BeanFactory实例化并注册BeanPostProcessor事件处理器  registerBeanPostProcessors(beanFactory);



BeanPostProcessor是Bean后置处理器，用于监听容器触发的事件

```java
public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// 从beanDefinitionMap中得到所有的BeanPostProcessor
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		// 注册了一个BeanPostProcessorChecker bean后置处理器
		// 当bean在实例化过程中没有执行后置处理器时会打印消息
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 分离 实现了PriorityOrdered、内部的、实现了Ordered、没有排序的后置处理器
   		// 分离是为了实现先后注册
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		// 遍历所有的bean后置处理器
		for (String ppName : postProcessorNames) {
			// 如果实现了PriorityOrdered接口
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				// 加入到priorityOrderedPostProcessors中
				priorityOrderedPostProcessors.add(pp);
				// 内部的后置处理器
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			// 如果实现了Ordered
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			// 没有进行排序的
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		// 首先，注册实现PriorityOrdered的BeanPostProcessors
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		// 接下来，注册实现Ordered的BeanPostProcessors
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		// 现在，注册所有常规beanpstprocessors
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		// 最后，重新注册所有内部beanpstprocessors
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		// 重新注册后处理器，以便将内部bean检测为应用程序侦听器，
		// 将其移动到处理器链的末端（用于获取代理等）
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}
```

实际上就是实例化几个Spring内部的bean后置处理器并把所有bean后置处理器向beanFactory注册，

注意这里的注册和之前注册beanDefinition不是一个意思，是将这些bean后置处理器专门放到一个beanFactory里的一个list当中，在bean的实例化过程中会循环这个list依次来执行这list里面的后置处理器，达到插手bean的实例化过程

```java
// 存放bean后置处理器的list
// 在bean的实例化过程中会循环这个list依次来执行这list里面的后置处理器，达到插手bean的实例化过程
private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
```



1.从beanDefinitionMap中得到所有的BeanPostProcessor

2.注册了一个BeanPostProcessorChecker bean后置处理器，当bean在实例化过程中没有执行后置处理器时会打印消息，注意这个后置处理器没有被放入beanDefinitionMap，是Spring直接注册的

3.分离 实现了PriorityOrdered、内部的、实现了Ordered、没有排序的后置处理器，分离是为了实现先后注册

4.首先，注册实现PriorityOrdered的BeanPostProcessors

5.接下来，注册实现Ordered的BeanPostProcessors

6.然后，注册所有常规beanpstprocessors

7.最后，重新注册所有内部beanpstprocessors

8.方法最后重新注册了一个ApplicationListenerDetector，将其移动到处理器链的末端（用于获取代理等），用于监听bean是否实现了ApplicationListener，如果实现了就加入当前的applicationContext的applicationListeners列表



重新注册之前：

![1584860903628](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584860903628.png)



重新注册后，移动到list的最后面：

![1584860938559](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1584860938559.png)



此时我们注意到该list里已经有了7个beanPostPorcessor，其中

1.ApplicationContextAwareProcessor 看5.3.1

2.ImportAwareBeanPostProcessor 看5.5.6.7

3.BeanPostProcessorChecker的作用上面已经说了

4.CommonAnnotationBeanPostProcessor 

​	用于处理@Resource、@PostConstruct、@PreDestroy注解，@Resource由他自己处理，两外两个由其父类

​	InitDestroyAnnotationBeanPostProcessor的postProcessMergedBeanDefinition方法来处理

5.AutowiredAnnotationBeanPostProcessor

​	处理bean中的@Autowired注解

6.RequiredAnnotationBeanPostProcessor

​	处理bean中的@Required注解

7.ApplicationListenerDetector

​	检查bean是否实现了ApplicationListener 如果实现了就加入当前的applicationContext的applicationListeners列表





======================================我是分隔符(*^__^*) ===================================





###5.7  初始化此上下文的消息源，和国际化相关 initMessageSource();

```java
/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 * 初始化消息源
	 * 如果此上下文中未定义父项，则使用父项。
	 */
	protected void initMessageSource() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 如果bean工厂有叫messageSource的bean
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			// 取出该bean
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			// 判断是不是HierarchicalMessageSource类型的
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				// 仅当没有父消息源时才将父上下文设置为父消息源
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// 否则新建一个DelegatingMessageSource作为messageSource 放入到工厂中
			// Use empty MessageSource to be able to accept getMessage calls.
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MessageSource with name '" + MESSAGE_SOURCE_BEAN_NAME +
						"': using default [" + this.messageSource + "]");
			}
		}
	}

```

这个方法不去做过多分析了，不重要，看看代码里注释就行





###5.8 初始化容器应用事件传播器 initApplicationEventMulticaster();

初始化ApplicationEventMulticaster。

如果上下文中未定义，则使用SimpleApplicationEventMulticaster。





###5.9 调用子类的某些特殊Bean初始化方法 onRefresh();

目前是一个空壳方法，可以在Spring实例化单例的bean之前，实例化一些特殊的bean





###5.10 为事件传播器注册事件监听器 registerListeners();

添加实现了ApplicationListener接口的bean作为监听器不会影响其他添加了但没有变成bean的监听器

1.首先注册静态指定的侦听器

2.从bean工厂取出已经实现了ApplicationListener接口的bean，并注册为ListenerBean

3.清空早期发布的事件





### 5.11 初始化所有剩余的单例Bean finishBeanFactoryInitialization(beanFactory);

https://blog.csdn.net/jy02268879/article/details/87918404

https://blog.csdn.net/qq_34886352/article/details/88865235

InstantiationAwareBeanPostProcessor

https://blog.csdn.net/u010634066/article/details/80321854

https://blog.csdn.net/yjn1995/article/details/95042102



合并子父bean定义

https://blog.csdn.net/andy_zhang2007/article/details/86514320



推断构造

https://cloud.tencent.com/developer/article/1518801

https://www.jianshu.com/p/36b42c329f8a

https://www.jianshu.com/p/885c2389b0e4

https://www.jianshu.com/p/c8a271448dcd

https://blog.csdn.net/finalcola/article/details/81451019



循环依赖

https://www.jianshu.com/p/8bb67ca11831



【Spring源码分析】非懒加载的单例Bean初始化过程（上篇）

https://www.cnblogs.com/xrq730/p/6361578.html

https://blog.csdn.net/andy_zhang2007/article/details/86514320

doCreateBean

https://blog.csdn.net/jy02268879/article/details/87940150?depth_1-utm_source=distribute.pc_relevant.none-task&utm_source=distribute.pc_relevant.none-task





```java
/**
	 * Finish the initialization of this context's bean factory,
	 * initializing all remaining singleton beans.
	 */
	// 对配置了lazy-init属性的Bean进行预实例化处理
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		// 这是Spring3以后新加的代码，为容器指定一个转换服务(ConversionService)
		// 在对某些Bean属性进行转换时使用
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Register a default embedded value resolver if no bean post-processor
		// (such as a PropertyPlaceholderConfigurer bean) registered any before:
		// at this point, primarily for resolution in annotation attribute values.
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// Stop using the temporary ClassLoader for type matching.
		// 为了类型匹配，停止使用临时的类加载器
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		// 缓存容器中所有注册的BeanDefinition元数据，以防被修改
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		// 实例化所有的单例对象
		beanFactory.preInstantiateSingletons();
	}

```



主要关注最后一行代码beanFactory.preInstantiateSingletons();===>



```java
@Override
	public void preInstantiateSingletons() throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Pre-instantiating singletons in " + this);
		}

		// Iterate over a copy to allow for init methods which in turn register new bean definitions.
		// While this may not be part of the regular factory bootstrap, it does otherwise work fine.
		// 所有bean的名字
		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

		// Trigger initialization of all non-lazy singleton beans...
		// 触发所有非延迟加载单例beans的初始化，主要步骤为调用getBean
		for (String beanName : beanNames) {
			// 获取指定名称的Bean定义 如果有parent 则合并父BeanDefinition
            RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			// 如果不是抽象的，且是Singleton的，且不是懒加载的
			if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
				// 如果是FactoryBean
				if (isFactoryBean(beanName)) {
					// FACTORY_BEAN_PREFIX=”&”，当Bean名称前面加”&”符号
					// 时，获取的是产生容器对象本身，而不是容器产生的Bean.
					// 调用getBean方法，触发容器对Bean实例化和依赖注入过程
					Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
					if (bean instanceof FactoryBean) {
						final FactoryBean<?> factory = (FactoryBean<?>) bean;
						// 标识是否需要预实例化
						boolean isEagerInit;
						if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
							// 一个匿名内部类
							isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>)
											((SmartFactoryBean<?>) factory)::isEagerInit,
									getAccessControlContext());
						}
						else {
							isEagerInit = (factory instanceof SmartFactoryBean &&
									((SmartFactoryBean<?>) factory).isEagerInit());
						}
						if (isEagerInit) {
							// 调用getBean方法，触发容器对Bean实例化和依赖注入过程
							getBean(beanName);
						}
					}
				}
				else {
					// 不是FactoryBean
					getBean(beanName);
				}
			}
		}
```



1.根据beanName获取RootBeanDefinition类型的bean定义，并且如果有parent，还会进行合并

2.判断如果bean不是抽象类，且是单例的，且不是懒加载的这里才会进行初始化

3.判断是否是FactoryBean

4.其实最终就是调用了getBean去做初始化



======================================我是分隔符(*^__^*) ===================================



####5.11.1 合并子bean和父bean的定义 getMergedLocalBeanDefinition(beanName) 方法说明与解析

这个方法后面会出现多次，所以先拿出来分析下，作用就是根据beanName得到一个RootBeanDefinition类型的bean定义，如果当前bean有parent，则合并后返回。

MergedLocalBeanDefinition的意思就是''合并后的''RootBeanDefinition，同时Spring也提供了MergedBeanDefinitionPostProcessor这个后置处理回调接口，如果通过这个回调修改了MergedLocalBeanDefinition里的属性值的话，是不影响原生的bean的

```java
/*
* 获取指定名称的Bean定义并转为RootBeanDefinition类型 如果有parent 则合并父BeanDefinition
*/
protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		// Quick check on the concurrent map first, with minimal locking.
		// 首先从缓存中找是否已经有缓存了的bean定义
		// mergedBeanDefinitions里放的就是合并后的bean定义
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null) {
			return mbd;
		}
		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
	}
```

1.如果之前合并过会被放入缓存所以首先从缓存中找

2.获取bean定义后，进行合并



合并过程===>

```java
protected RootBeanDefinition getMergedBeanDefinition(
			String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
			throws BeanDefinitionStoreException {

		// 防止并发合并
		synchronized (this.mergedBeanDefinitions) {
			RootBeanDefinition mbd = null;

			// Check with full lock now in order to enforce the same merged instance.
			if (containingBd == null) {
				mbd = this.mergedBeanDefinitions.get(beanName);
			}

			if (mbd == null) {
				// 如果没有parnet
				if (bd.getParentName() == null) {
					// Use copy of given root bean definition.
					// 如果BeanDefinition已经是RootBeanDefinition则克隆一个mbd
					if (bd instanceof RootBeanDefinition) {
						mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
					}
					else {
						// 如果不是则包装成RootBeanDefinition
						mbd = new RootBeanDefinition(bd);
					}
				}
				else {
					// Child bean definition: needs to be merged with parent.
					// 子bean与父bean的定义合并
					BeanDefinition pbd;
					try {
						// 通过name获取beanName,主要就是处理工厂解引用前缀或别名解析为规范名称
						String parentBeanName = transformedBeanName(bd.getParentName());
						/**
						 * 因为上面判断了parentBeanName是否为null，因此这里的parentBeanName肯定是有值的
						 * 而又去判断当前的beanName是否等于parentBeanName我觉得可能是Spring做的严谨的判断
						 */
						if (!beanName.equals(parentBeanName)) {
							// 递归，因为parent也可能是一个child
							pbd = getMergedBeanDefinition(parentBeanName);
						}
						else {
							// 得到parent的bean工厂
							BeanFactory parent = getParentBeanFactory();
							if (parent instanceof ConfigurableBeanFactory) {
								// 递归得到parent合并后的定义
								pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
							}
							else {
								throw new NoSuchBeanDefinitionException(parentBeanName,
										"Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
										"': cannot be resolved without an AbstractBeanFactory parent");
							}
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
								"Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
					}
					// Deep copy with overridden values.
					// 将合并后的定义包装成RootBeanDefinition
					mbd = new RootBeanDefinition(pbd);
					// 将mbd里的属性值覆写为bd里的属性值
					mbd.overrideFrom(bd);
				}

				// Set default singleton scope, if not configured before.
				// 设置作用域，如果合并后没有，默认单例
				if (!StringUtils.hasLength(mbd.getScope())) {
					mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
				}

				// A bean contained in a non-singleton bean cannot be a singleton itself.
				// Let's correct this on the fly here, since this might be the result of
				// parent-child merging for the outer bean, in which case the original inner bean
				// definition will not have inherited the merged outer bean's singleton status.
				// 包含在非单例bean中的内部bean本身不能是单例bean。
				// 如果有内部bean，并且内部bean不是单例的，并且外部bean是单例的时候，矫正外部bean的作用域
				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
					mbd.setScope(containingBd.getScope());
				}

				// Cache the merged bean definition for the time being
				// (it might still get re-merged later on in order to pick up metadata changes)
				// 暂时缓存合并的bean定义 （稍后可能仍会重新合并以获取元数据更改）
				if (containingBd == null && isCacheBeanMetadata()) {
					this.mergedBeanDefinitions.put(beanName, mbd);
				}
			}

			return mbd;
		}
	}
```

1.对最后要缓存合并后的定义的容器加锁，防止并发的情况，虽然容器本身是ConcurrentHashMap，已经是线程安全的，但会出现并发合并的情况，这里要防止的就是合并的过程出现并发问题

2.从容器中get一下，如果能get到，说明并发情况下已经被其他线程合并过了(因为多个线程进来，只有一个线程会进同步代码块，等该线程释放锁后，别的线程就来就能get到，那么就不会出现多次合并的情况)

3.如果没有parnet

​	3.1 如果BeanDefinition已经是RootBeanDefinition则克隆一个mbd出来

​	3.2 如果不是则直接新包装成RootBeanDefinition赋值给变量mbd

4.如果有parent，就先通过parentName获取beanName

​	4.1 如果当前bean的parent也有parent，就要去递归进行处理

​	4.2 得到最后合并后的mbd

​	4.3 将mbd里的属性值覆写为当前bean定义里的属性值，也就是子bean覆盖父bean的

5.如果合并后的定义里没有作用域，则默认为单例

6.如果有内部bean，并且内部bean不是单例的，而外部bean是单例的时候，矫正外部bean的作用域

7.暂时缓存合并的bean定义 （稍后可能仍会重新合并以获取元数据更改）



======================================我是分隔符(*^__^*) ===================================



####5.11.2 transformedBeanName()方法说明与解析

这个方法因为后面会出现多次，所以说明下作用，当我们根据从容器中获取bean的时候调用这个方法主要就是处理工厂解引用前缀 或 将别名解析为规范名称

```java
protected String transformedBeanName(String name) {
		return canonicalName(BeanFactoryUtils.transformedBeanName(name));
	}
```



1.将别名解析为规范名称 canonicalName()==> 就是从aliasMap里取出规范的beanName

```java
public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing...
		String resolvedName;
		do {
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				canonicalName = resolvedName;
			}
		}
		while (resolvedName != null);
		return canonicalName;
	}
```





2.name 可能会以 & 字符开头，表明调用者想获取 FactoryBean 本身，而非 FactoryBean  实现类所创建的 bean。在 BeanFactory 中，FactoryBean 的实现类和其他的 bean 存储  方式是一致的，即 <beanName, bean>，beanName 中是没有 & 这个字符的。所以我们需要  将 name 的首字符 & 移除，这样才能从缓存里取到 FactoryBean 实例。



BeanFactoryUtils.transformedBeanName(name)===>  其实就是移除首字符 &

```java
public static String transformedBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		String beanName = name;
		while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
			beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
		}
		return beanName;
	}
```





======================================我是分隔符(*^__^*) ===================================



####5.11.3 getBean()方法解析

```java
@Override
public Object getBean(String name) throws BeansException {
    return doGetBean(name, null, null, false);
}
```

套娃方法，调用doGetBean==>



```java
@SuppressWarnings("unchecked")
	protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
			@Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

		/**
		 * 获取beanName
		 */
		final String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		/**
		 * 看是否命中缓存，实例化过程中肯定为null
		 */
		Object sharedInstance = getSingleton(beanName);
		// 如果命中缓存
		if (sharedInstance != null && args == null) {
			if (logger.isDebugEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				}
				else {
					logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			/**
			 * 如果 sharedInstance 是普通的单例 bean，下面的方法会直接返回。但如果
			 * sharedInstance 是 FactoryBean 类型的，则需调用 getObject 工厂方法获取真正的
			 * bean 实例。如果用户想获取 FactoryBean 本身，这里也不会做特别的处理，直接返回
			 * 即可。毕竟 FactoryBean 的实现类本身也是一种 bean，只不过具有一点特殊的功能而已。
			 */
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}

		else {
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.

			// 缓存没有正在创建的单例模式Bean
			// 缓存中已经有已经创建的原型模式Bean
			// 但是由于循环引用的问题导致实例化对象失败

			// 只有在单例情况下才会尝试解决循环依赖，原型模式情况下，如果存在
			// A中有B的属性，B中有A的属性，那么当依赖注入的时候，就会产生当A还未创建完的时候因为
			// 对于B的创建再次返回创建A，造成循环依赖，也就是下面的情况
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// Check if bean definition exists in this factory.
			// 对IOC容器中是否存在指定名称的BeanDefinition进行检查，首先检查是否
			// 能在当前的BeanFactory中获取的所需要的Bean，如果不能则委托当前容器
			// 的父级容器去查找，如果还是找不到则沿着容器的继承体系向父级容器查找
			BeanFactory parentBeanFactory = getParentBeanFactory();
			// 当前容器的父级容器存在，且当前容器中不存在指定名称的Bean
			// 如果beanDefinitionMap中也就是在所有已加载的类中不包括beanName则尝试从
			// parentBeanFactory中检测
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
				// 解析指定Bean名称的原始名称
				String nameToLookup = originalBeanName(name);
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
							nameToLookup, requiredType, args, typeCheckOnly);
				}
				else if (args != null) {
					// Delegation to parent with explicit args.
					// 委派父级容器根据指定名称和显式的参数查找
					// 递归到BeanFactory中寻找
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				}
				else {
					// No args -> delegate to standard getBean method.
					// 委派父级容器根据指定名称和类型查找
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
			}

			// 创建的Bean是否需要进行类型验证，一般不需要
			// 如果不是仅仅做类型检查则是创建bean，这里要进行记录
			if (!typeCheckOnly) {
				// 添加到alreadyCreated set集合当中
                // 向容器标记指定的Bean至少被创建过一次，以避免重复创建指定的bean
				markBeanAsCreated(beanName);
			}

			try {
				// 根据指定Bean名称获取其父级的Bean定义
				// 主要解决Bean继承时子类合并父类公共属性问题

				// 将储存XML配置文件的GernericBeanDefinition转换为RootBeanDefinition，如果指定
				// BeanName是子Bean的话会合并父类的相关属性
				final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				// 检察合并后描述的bean是否是抽象的
				checkMergedBeanDefinition(mbd, beanName, args);

				// Guarantee initialization of beans that the current bean depends on.
				// 获取当前Bean所有依赖Bean的名称
				String[] dependsOn = mbd.getDependsOn();
				// 如果当前Bean有依赖Bean，递归
				if (dependsOn != null) {
					for (String dep : dependsOn) {
						if (isDependent(beanName, dep)) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}
						// 递归调用getBean方法，获取当前Bean的依赖Bean
						registerDependentBean(dep, beanName);
						try {
							// 把被依赖Bean注册给当前依赖的Bean
							getBean(dep);
						}
						catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
				}

				// Create bean instance.
				// 创建单例模式Bean的实例对象
				// 实例化依赖的bean后便可以实例化mbd本身了
				if (mbd.isSingleton()) {
					// 这里使用了一个匿名内部类，创建Bean实例对象，并且注册给所依赖的对象
					sharedInstance = getSingleton(beanName, () -> {
						try {
							// 创建一个指定Bean实例对象，如果有父级继承，则合并子类和父类的定义
							return createBean(beanName, mbd, args);
						}
						catch (BeansException ex) {
							// Explicitly remove instance from singleton cache: It might have been put there
							// eagerly by the creation process, to allow for circular reference resolution.
							// Also remove any beans that received a temporary reference to the bean.
							// 显式地从容器单例模式Bean缓存中清除实例对象
							destroySingleton(beanName);
							throw ex;
						}
					});
					// 获取给定Bean的实例对象
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}

				// IOC容器创建原型模式Bean实例对象
				else if (mbd.isPrototype()) {
					// It's a prototype -> create a new instance.
					// 原型模式(Prototype)是每次都会创建一个新的对象
					Object prototypeInstance = null;
					try {
						// 回调beforePrototypeCreation方法，默认的功能是注册当前创建的原型对象
						beforePrototypeCreation(beanName);
						// 创建指定Bean对象实例
						prototypeInstance = createBean(beanName, mbd, args);
					}
					finally {
						// 回调afterPrototypeCreation方法，默认的功能告诉IOC容器指定Bean的原型对象不再创建
						afterPrototypeCreation(beanName);
					}
					// 获取给定Bean的实例对象
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}

				// 要创建的Bean既不是单例模式，也不是原型模式，则根据Bean定义资源中
				// 配置的生命周期范围，选择实例化Bean的合适方法，这种在Web应用程序中
				// 比较常用，如：request、session、application等生命周期
				else {
					String scopeName = mbd.getScope();
					final Scope scope = this.scopes.get(scopeName);
					// Bean定义资源中没有配置生命周期范围，则Bean定义不合法
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}
					try {
						// 这里又使用了一个匿名内部类，获取一个指定生命周期范围的实例
						Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							}
							finally {
								afterPrototypeCreation(beanName);
							}
						});
						// 获取给定Bean的实例对象
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					}
					catch (IllegalStateException ex) {
						throw new BeanCreationException(beanName,
								"Scope '" + scopeName + "' is not active for the current thread; consider " +
								"defining a scoped proxy for this bean if you intend to refer to it from a singleton",
								ex);
					}
				}
			}
			catch (BeansException ex) {
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}
		}

		// Check if required type matches the type of the actual bean instance.
		// 对创建的Bean实例对象进行类型检查
		if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return convertedBean;
			}
			catch (TypeMismatchException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to convert bean '" + name + "' to required type '" +
							ClassUtils.getQualifiedName(requiredType) + "'", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

```



1.根据传入的name，获取beanName

2.根据beanName查看是否命中缓存

​	2.1 如果命中缓存则获取给定bean实例的对象，返回实例本身或其创建的对象（以FactoryBean为例）,

​		该方法下面会有解析

3.如果 没有命中缓存，判断当前bean是否被标识为正在创建的原型bean，如果是则抛出异常，这种情况是原型bean循环依赖的时候发生

4.如果存在父容器，且当前子容器没有给定beanName的bean定义，则委派父级容器去getBean来得到对象实例

5.如果typeCheckOnly为false，则向容器标记指定的Bean至少被创建过一次，添加到alreadyCreated set集合当中，表示他已经创建过一场，以避免重复创建指定的bean

6.根据beanName得到RootBeanDefinition类型的bean定义，如果存在parent，会进行合并定义，然后缓存起来

7.检察合并后描述的bean是否是抽象的，如果是则抛出异常

8.如果当前bean依赖了其他bean，就注册依赖关系，然后递归getBean，获取当前Bean的依赖Bean

9.得到bean的实例

​	9.1 如果是单例的，则将该bean标记为正在创建，然后调用AbstractAutowireCapableBeanFactory类的	createBean()方法创建实例化对象，实例化完后，从容器中获取bean实例

​	9.2 如果是原型的，则先回调beforePrototypeCreation方法，默认的功能是注册当前创建的原型对象，然后调用createBean()方法创建实例对象，再回调afterPrototypeCreation方法，默认的功能告诉IOC容器指定bean的原型对象不再创建，最后从容器中获取bean实例

​	9.3 既不是单例，也不是原型，则根据则根据bean定义中的scope来选择策略创建实例，先回调beforePrototypeCreation方法，然后调用createBean()方法创建实例对象，再回调调用afterPrototypeCreation方法，最后从容器中获取bean

10.检测需要的类型是否符合Bean的实际类型，并返回





======================================我是分隔符(*^__^*) ====================================



####5.11.4 DefaultSingletonBeanRegistry类getSingleton(String beanName, boolean allowEarlyReference)方法解析

在getBean中获取缓存就是通过DefaultSingletonBeanRegistry类的getSingleton(String beanName, boolean allowEarlyReference) 方法来的

```java
@Override
@Nullable
public Object getSingleton(String beanName) {
    return getSingleton(beanName, true);
}
```



```java
/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 *
	 * 这个方法是用于我们在getBean的时候来命中缓存的
	 */
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		//从map中获取bean如果不为空直接返回，不再进行初始化工作
		Object singletonObject = this.singletonObjects.get(beanName);
		//从单例bean缓存中读取为null且判断在建中的bean中也没有的话，存入缓存
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			//如果为空，则锁定全局变量并进行处理
			synchronized (this.singletonObjects) {
				//如果此bean正在加载则不处理
				singletonObject = this.earlySingletonObjects.get(beanName);
				//allowEarlyReference从前面的方法传递过来的时候默认为true
				if (singletonObject == null && allowEarlyReference) {
					//当某些方法需要提前初始化的时候会调用addSingletonFactory将对应的
					//objectFactory初始化策略存储在singletonFactories，所以这里判断下有没有被初始化过
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						singletonObject = singletonFactory.getObject();
						//记录在缓存中，earlySingletonObjects和singletonFactories互斥
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return singletonObject;
	}
```

1.从工厂中的singletonObjects这个map中获取bean如果不为空直接返回，不再进行初始化工作

```java
/** Cache of singleton objects: bean name --> bean instance */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
```

2.如果缓存没有命中 且 bean正在创建，加锁

3.判断该bean是否存在早期单例对象缓存中(earlySingletonObjects )，如果有就取出返回

4.如果没有 且 允许创建早期对象，则判断是否在单例工厂(singletonFactorieS)中存在

5.如果存在则创建该bean的实例，然后将该bean存入earlySingletonObjects，且从singletonFactories中移除，最后返回该bean的实例



这种情况会在循环引用时发生





======================================我是分隔符(*^__^*) ====================================



####5.11.5  获取给定bean的对象实例getObjectForBeanInstance方法说明与解析

入口==>doGetBean中的

```
bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
```

作用:

1.如果sharedInstance是个普通bean会直接返回

2.如果sharedInstance 是FactoryBean类型的，则需调用getObject方法获取真正的bean

3.如果用户想获取FactoryBean本身，这里也不会做特别的处理，直接返回



```java
protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		// Don't let calling code try to dereference the factory if the bean isn't a factory.
		// 如果是工厂的间接引用
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			if (beanInstance instanceof NullBean) {
				return beanInstance;
			}
			// 如果是工厂的间接引用 但不是FactoryBean类型的则抛出异常
			if (!(beanInstance instanceof FactoryBean)) {
				throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
			}
		}

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
		// If it's a FactoryBean, we use it to create a bean instance, unless the
		// caller actually wants a reference to the factory.
		// 现在我们有了bean实例，它可能是一个普通bean或一个Factorybean。
		// 如果它是一个FactoryBean，我们使用它来创建一个bean实例，除非实际上想要一个工厂的引用
		// 如果beanInstance不是FactoryBean类型的就说明是一个普通bean 或者 传入的name是以&开头的就说明要的是Factorybean
		if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
			return beanInstance;
		}

		Object object = null;
		if (mbd == null) {
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// Return bean instance from factory.
			// 从工厂返回bean实例
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// Caches object obtained from FactoryBean if it is a singleton.
			// 缓存从FactoryBean获取的对象（如果它是单例对象）
			if (mbd == null && containsBeanDefinition(beanName)) {
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			// 从给定的FactoryBean获取要公开的对象
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}
```

1.判断beanInstance如果是FactoryBean的间接引用(name 以 & 开头)，但不是FactoryBean类型的则抛出异常

2.如果beanInstance不是FactoryBean类型的就说明是一个普通bean，直接返回，或者我们要的就是FactoryBean本身(根据传入的name判断)，也直接返回

3.如果mdb为空，则从缓存中获取FactoryBean要返回的对象

4.如果缓存中没有，则从给定的FactoryBean获取要公开的对象，且放入缓存，最后返回





======================================我是分隔符(*^__^*) ====================================



####5.11.6 创建单例模式Bean的实例对象

```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		synchronized (this.singletonObjects) {
			Object singletonObject = this.singletonObjects.get(beanName);
			// 如果为null，表示Spring需要创建对象
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				/**
				 * 将beanName添加到singletonsCurrentlyInCreation这样一个set集合中
				 * 表示beanName对应的bean正在创建中
				 */
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					// 调用AbstractAutowireCapableBeanFactory类的createBean()方法创建实例化对象
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					// 单例bean的实例创建好后，把标识为正在创建的标识去掉
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					// 将实例化好后的对象放入beanFactory的缓存singletonObjects中
					// 放入beanFactory的registeredSingletons中
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}

```

1.加锁后尝试从缓存中去一下，取不到就将beanName添加到singletonsCurrentlyInCreation这样一个set集合中，表示beanName对应的bean正在创建中

2.调用AbstractAutowireCapableBeanFactory类的createBean()方法创建实例化对象

3.单例bean的实例创建好后，把标识为正在创建的标识去掉

4.将实例化好后的对象放入beanFactory的缓存singletonObjects 和 registeredSingletons中

```java
protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.put(beanName, singletonObject);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}
```





======================================我是分隔符(*^__^*) ====================================



#####5.11.5.1 AbstractAutowireCapableBeanFactory#createBean()

**从createBean开始，Spring Bean的生命周期中共调用了9次，5个bean的后置处理器**，关于beanPostProcessor的具体解析看后面的**Bean的生命周期**



createBean()方法里，resolveBeforeInstantiation()第一次调用了Bean的后置处理器，这个方法是在调用doCreateBean()之前执行的

```
Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
```



#####5.11.6.2 AbstractAutowireCapableBeanFactory#doCreateBean()方法解析

该方法是真正来创建bean的

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

		// Instantiate the bean.
		// 封装被创建的Bean对象
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			// 创建bean的实例
            /**
			 * 创建 bean 实例，并将实例包裹在 BeanWrapper 实现类对象中返回。
			 * createBeanInstance中包含三种创建 bean 实例的方式：
			 *   1. 通过工厂方法创建 bean 实例
			 *   2. 通过构造方法自动注入（autowire by constructor）的方式创建 bean 实例
			 *   3. 通过无参构造方法方法创建 bean 实例
			 *
			 * 若 bean 的配置信息中配置了 lookup-method 和 replace-method，则会使用 CGLIB
			 * 增强 bean 实例。
			 */
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		final Object bean = instanceWrapper.getWrappedInstance();
		//获取实例化对象的类型
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		// 允许后置处理器修改合并bean定义
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
                    // 第三次，执行bean的后置处理器
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		//向容器中缓存单例模式的Bean对象，以防循环引用
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			// 为了防止循环引用，尽早持有对象的引用
            // getEarlyBeanReference()这是第四次，执行bean的后置处理器
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// Initialize the bean instance.
		//Bean对象的初始化，依赖注入在此触发
		//这个exposedObject在初始化完成之后返回作为依赖注入完成后的Bean
		Object exposedObject = bean;
		try {
			// 设置属性，非常重要，这里面是第五和第六次执行后置处理器
			// 将Bean实例对象封装，并且Bean定义中配置的属性值赋值给实例对象
			populateBean(beanName, mbd, instanceWrapper);
			//这里面是第七次，执行后置处理器，aop就是在这里完成的处理
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			//获取指定名称的已注册的单例模式Bean对象
			Object earlySingletonReference = getSingleton(beanName, false);
			if (earlySingletonReference != null) {
				//根据名称获取的已注册的Bean和正在实例化的Bean是同一个
				if (exposedObject == bean) {
					//当前实例化的Bean初始化完成
					exposedObject = earlySingletonReference;
				}
				//当前Bean依赖其他Bean，并且当发生循环引用时不允许新创建实例对象
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					//获取当前Bean所依赖的其他Bean
					for (String dependentBean : dependentBeans) {
						//对依赖Bean进行类型检查
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		//注册完成依赖注入的Bean
		try {
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}

```

1.封装被创建的Bean对象，并调用createBeanInstance()去创建实例，是个没有填充属性的实例

2.将MergedBeanDefinitionPostProcessors应用于指定的bean定义，调用它们的postprocessemergedbeandefinition方法，**这是spring在bean的生命周期中第三次调用bean的后置处理器**

3.向容器中缓存单例模式的Bean对象，尽早持有对象的引用，以防循环依赖，**这是spring在bean的生命周期中第四次调用bean的后置处理器**

4.调用populateBean()方法给bean实例填充属性，**里面是第五和第六次调用bean的后置处理器**

5.调用initializeBean()方法初始化bean，完成bean的生命周期回调，AOP就是在此完成，**里面是第七次调用bean的后置处理器**

7.如果需要提前暴露，处理循环依赖

8.注册bean的销毁方法，在工厂关闭时调用，只针对单例的情况





======================================我是分隔符(*^__^*) ====================================



#####5.11.6.3 实际创建Bean的实例对象 createBeanInstance()方法解析

创建 bean 实例，并将实例包裹在 BeanWrapper 实现类对象中返回。
createBeanInstance中包含三种创建 bean 实例的方式：

  1. 通过工厂方法创建 bean 实例
  2. 通过构造方法自动注入（autowire by constructor）的方式创建 bean 实例
  3. 通过无参构造方法方法创建 bean 实例

```
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
		// Make sure bean class is actually resolved at this point.
		//检查确认Bean是可实例化的
		Class<?> beanClass = resolveBeanClass(mbd, beanName);

		/**
		 * 检测一个类的访问权限spring默认情况下对于非public的类是允许访问的。
		 */
		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}

		// 处理通过Supplier函数式接口来实例化对象，如果有就直接return了
		Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
		if (instanceSupplier != null) {
			return obtainFromSupplier(instanceSupplier, beanName);
		}

		/**
		 * 如果工厂方法不为空，则通过工厂方法构建 bean 对象
		 * xml:factory-method
		 * anno:@Bean
		 */
		if (mbd.getFactoryMethodName() != null) {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		// Shortcut when re-creating the same bean...
		//使用容器的自动装配方法进行实例化
		/**
		 * 从spring的原始注释可以知道这个是一个Shortcut，什么意思呢？
		 * 当多次构建同一个 bean 时，可以使用这个Shortcut，
		 * 也就是说不在需要次推断应该使用哪种方式构造bean
		 *  比如在多次构建同一个prototype类型的 bean 时，就可以走此处的shortcut
		 * 这里的 resolved 和 mbd.constructorArgumentsResolved 将会在 bean 第一次实例
		 * 化的过程中被设置，后面来证明
		 */
		boolean resolved = false;
		boolean autowireNecessary = false;
		if (args == null) {
			synchronized (mbd.constructorArgumentLock) {
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					resolved = true;
					// 如果已经解析了构造方法的参数，则必须要通过一个带参构造方法来实例
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		if (resolved) {
			if (autowireNecessary) {
				//配置了自动装配属性，使用容器的自动装配实例化
				//容器的自动装配是根据参数类型匹配Bean的构造方法
				// 通过构造方法自动装配的方式构造 bean 对象
				return autowireConstructor(beanName, mbd, null, null);
			}
			else {
				// 调用无参构造方法实例化
				return instantiateBean(beanName, mbd);
			}
		}

		// Candidate constructors for autowiring?
		// 由后置处理器决定返回哪些构造方法
		// 使用Bean的构造方法进行实例化 需要推断构造方法，该方法是获取所有@Autowire构造方法的
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			// 使用容器的自动装配特性，调用匹配的有参构造方法实例化
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// No special handling: simply use no-arg constructor.
		// 使用默认的无参构造方法实例化
		return instantiateBean(beanName, mbd);
	}
```

1.确保bean被解析过

2.如果bean不是public 且 配置了Spring不能访问非public的bean，则抛异常

3.如果工厂方法不为空，则通过工厂方法构建 bean 对象

4.判断能否通过快捷方式来实例化对象

​	因为在实例化对象的时候需要推断使用哪个构造方法来实例化，这个过程很复杂，所以Spring会将解析后确定好的构造方法缓存到RootBeanDefinition中(resolvedConstructorOrFactoryMethod)，那么当多次构建同一个 bean 时可以使用这个快捷方式，比如构建同一个prototype类型的bean

7.通过后置处理器决定候选构造函数列表

6.如果构造参数候选列表不为空 || bean的装配模式是构造注入 || bean的定义中有构造参数值 || 传入了参数列表 那么就通过构造方法进行构造推断，实例化与自动装配，否则使用无参构造方法实例化



======================================我是分隔符(*^__^*) ====================================



###5.12 实例化bean时Spring是如何推断构造方法的?

####5.12.1 先通过后置处理器决定候选构造函数列表

入口AbstractAutowireCapableBeanFactory，这是Spring Bean的生命周期中，**第二次调用bean的后置处理器**

```java
Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
```



```java
protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName)
			throws BeansException {

		if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					Constructor<?>[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
					if (ctors != null) {
						return ctors;
					}
				}
			}
		}
		return null;
	}
```

其实就是通过后置处理器来解析bean里的构造方法，最终会调用到AutowiredAnnotationBeanPostProcessor类中的determineCandidateConstructors方法



**先根据debug结果看其是怎么选择的:**

**一.没有加了@Autowired注解的构造，不管怎么样都会返回null**

![1585242242107](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1585242242107.png)

​	

但是如果有带参构造，但没有无参构，那么后面实例化的时候若被Spring判断为采用无参构造进行实例化，就会产生异常，因为没有无参构造。至于为什么可能被判断为无参构造进行实例化上面5.11.6.2分析过了。

![1585242329893](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1585242329893.png)

由于找不到无参构造方法，实例化的时候产生异常

![1585242375938](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1585242375938.png)



**二.加了@Autowired注解的构造**

有三个构造，有一个加了@Autowired(required = false)，将无参和加了注解的返回

![1585242595106](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1585242595106.png)



三个构造，两个带参的加了@Autowired(required = false)，就都返回了

![1585242722076](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1585242722076.png)

但是如果有多个@Autowired，即使只有一个required=true，其他都是false也会产生异常



下面我来通过源码分析一下===>

```java
public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
			throws BeanCreationException {

		// Let's check for lookup methods here...
		if (!this.lookupMethodsChecked.contains(beanName)) {
			try {
				ReflectionUtils.doWithMethods(beanClass, method -> {
					Lookup lookup = method.getAnnotation(Lookup.class);
					if (lookup != null) {
						Assert.state(this.beanFactory != null, "No BeanFactory available");
						LookupOverride override = new LookupOverride(method, lookup.value());
						try {
							RootBeanDefinition mbd = (RootBeanDefinition)
									this.beanFactory.getMergedBeanDefinition(beanName);
							mbd.getMethodOverrides().addOverride(override);
						}
						catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(beanName,
									"Cannot apply @Lookup to beans without corresponding bean definition");
						}
					}
				});
			}
			catch (IllegalStateException ex) {
				throw new BeanCreationException(beanName, "Lookup method resolution failed", ex);
			}
			this.lookupMethodsChecked.add(beanName);
		}

		//首先从容器的缓存中查找是否有指定Bean的构造方法
		// Quick check on the concurrent map first, with minimal locking.
		Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
		if (candidateConstructors == null) {
			// Fully synchronized resolution now...
			//线程同步以确保容器中数据一致性
			synchronized (this.candidateConstructorsCache) {
				candidateConstructors = this.candidateConstructorsCache.get(beanClass);
				if (candidateConstructors == null) {
					Constructor<?>[] rawCandidates;
					try {
						//通过JDK反射机制，获取指定类的中所有声明的构造方法
						rawCandidates = beanClass.getDeclaredConstructors();
					}
					catch (Throwable ex) {
						throw new BeanCreationException(beanName,
								"Resolution of declared constructors on bean Class [" + beanClass.getName() +
								"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
					}
					//存放候选构造方法的集合
					List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
					//autowire注解中required属性指定的构造方法
					Constructor<?> requiredConstructor = null;
					//默认的构造方法
					Constructor<?> defaultConstructor = null;
					// 对于Kotlin类，这个返回与Kotlin主构造函数对应的Java构造函数, 非Kotlin类都返回null，因此我们可以忽略
					Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(beanClass);
					int nonSyntheticConstructors = 0;
					//遍历所有的构造方法，检查是否添加了autowire注解，以及是否指定了required属性
					for (Constructor<?> candidate : rawCandidates) {
						if (!candidate.isSynthetic()) {
							nonSyntheticConstructors++;
						}
						else if (primaryConstructor != null) {
							continue;
						}
						// 获取该构造方法上的@Antowire注解信息
						AnnotationAttributes ann = findAutowiredAnnotation(candidate);
						// 没有@Antowire的注解
						if (ann == null) {
							// 用于判断是否是cglib的代理类
							Class<?> userClass = ClassUtils.getUserClass(beanClass);
							if (userClass != beanClass) {
								try {
									Constructor<?> superCtor =
											userClass.getDeclaredConstructor(candidate.getParameterTypes());
									ann = findAutowiredAnnotation(superCtor);
								}
								catch (NoSuchMethodException ex) {
									// Simply proceed, no equivalent superclass constructor found...
								}
							}
						}
						// 如果有@Antowire的注解
						if (ann != null) {
							//如果antowire注解中指定了required属性
							if (requiredConstructor != null) {
								throw new BeanCreationException(beanName,
										"Invalid autowire-marked constructor: " + candidate +
										". Found constructor with 'required' Autowired annotation already: " +
										requiredConstructor);
							}
							//获取autowire注解中required属性值
							boolean required = determineRequiredStatus(ann);
							//如果获取到autowire注解中required的属性值
							if (required) {
								//如果候选构造方法集合不为空
								if (!candidates.isEmpty()) {
									throw new BeanCreationException(beanName,
											"Invalid autowire-marked constructors: " + candidates +
											". Found constructor with 'required' Autowired annotation: " +
											candidate);
								}
								//当前的构造方法就是required属性所配置的构造方法
								requiredConstructor = candidate;
							}
							//将当前的构造方法添加到候选构造方法集合中
							candidates.add(candidate);
						}
						//如果autowire注解的参数列表为空，默认使用无参构造方法
						else if (candidate.getParameterCount() == 0) {
							defaultConstructor = candidate;
						}
					}
					//如果候选构造方法集合不为空
					if (!candidates.isEmpty()) {
						// Add default constructor to list of optional constructors, as fallback.
						//如果所有的构造方法都没有配置required属性，且有默认构造方法
						if (requiredConstructor == null) {
							if (defaultConstructor != null) {
								//将默认构造方法添加到候选构造方法列表
								candidates.add(defaultConstructor);
							}
							else if (candidates.size() == 1 && logger.isWarnEnabled()) {
								logger.warn("Inconsistent constructor declaration on bean with name '" + beanName +
										"': single autowire-marked constructor flagged as optional - " +
										"this constructor is effectively required since there is no " +
										"default constructor to fall back to: " + candidates.get(0));
							}
						}
						//将候选构造方法集合转换为数组
						candidateConstructors = candidates.toArray(new Constructor<?>[0]);
					}
					else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
						candidateConstructors = new Constructor<?>[] {rawCandidates[0]};
					}
					else if (nonSyntheticConstructors == 2 && primaryConstructor != null &&
							defaultConstructor != null && !primaryConstructor.equals(defaultConstructor)) {
						candidateConstructors = new Constructor<?>[] {primaryConstructor, defaultConstructor};
					}
					else if (nonSyntheticConstructors == 1 && primaryConstructor != null) {
						candidateConstructors = new Constructor<?>[] {primaryConstructor};
					}
					else {
						//如果候选构造方法集合为空，则创建一个空的数组
						candidateConstructors = new Constructor<?>[0];
					}
					//将类的候选构造方法集合存放到容器的缓存中
					this.candidateConstructorsCache.put(beanClass, candidateConstructors);
				}
			}
		}
		//返回指定类的候选构造方法数组，如果没有返回null
		return (candidateConstructors.length > 0 ? candidateConstructors : null);
	}
```
1.首先从缓存中找，是否已结解析过，如果有则直接返回

2.加锁，再从缓存获取，双重判断，防止并发问题

3.通过反射，获取指定类的中所有声明的构造方法

4.遍历获取的构造方法

​	4.1 nonSyntheticConstructors++，标记被解析过的构造方法个数

​	4.2 获取该构造方法上的@Antowire注解信息(其实还会获取@Value注解的)，将key和value封装成一个AnnotationAttributes对象返回

​		4.2.1 **如果没有@Antowired的注解**，就判断是否是cglib代理类，如果是代理类，则根据参数获得被代理类的构造，然后取出被代理类的该构造的注解信息

​		4.2.2 **如果有@Antowired的注解**

​				1.判断requiredConstructor是否为空，如果不为空直接抛异常

​				2.获取@Autowired注解中required的值，为true就判断候选构造方法集合是否为空，不为空直接抛异常

​				3.如果required为false就将requiredConstructor设置为当前的构造方法

​		4.2.3 如果当前构造方法是无参构造且没有加@Autowired，就标记为默认构造

​	4.3 将当前的构造方法添加到候选构造方法集合中



5.如果候选构造方法集合不为空 && 没有required为true的构造 && 有默认构造，将默认构造方法添加到候选构造方法列表，并转换为数组缓存起来，最后返回

6.如果该bean的构造方法只有一个，且参数大于0，将其缓存起来，最后返回

7.如果如果候选构造方法集合为空，也进行缓存，标记为null，最后返回null



注:这里面还有两个if判断涉及到了primaryConstructor这个属性，这个属性应该一直都是为null的

![1585247665741](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1585247665741.png)

这个参数是和Kotlin有关的，java这边都是返回的null，Spring源码注释里有说明

Return the primary constructor of the provided class. **For Kotlin classes**, thisreturns the Java constructor corresponding to the Kotlin primary constructor(as defined in the Kotlin specification). **Otherwise, in particular for non-Kotlinclasses, this simply returns {@code null}.**



**总结： Spring如何决定构造函数列表的?**

**1.如果没有被@Autowired标注的构造，就会返回null**

**2.如果有多个@Autowired(required = false)，只要没有true的，就会把无参构造和被标注了的带参构造都返回**

**3.如果有多个@Autowired，只要有一个required为true，都会抛异常**

**4.如果只有一个@Autowired，就返回那个构造**



======================================我是分隔符(*^__^*) ====================================



####5.12.2 Spring选择最合适的构造方法



```java
/*
	 * 1.创建 BeanWrapperImpl 对象
	 * 2.构造候选方法只有一个的情况，满足就构造
	 * 3.构造候选方法多个的情况，获取构造方法和参数列表，并排序
	 * 4.排序规则是，优先public和参数多的
	 * 5.如果有个public而且参数多于需要的参数，选之
	 * 6.其次，选择参数相等的，参数不足的直接忽略
	 * 7.参数相等的情况，做类型转化，计算一个typeDiffWeight，相似参数的度量，选择最相似的，如果多个typeDiffWeight相等，那么报错。
	 * 8.最后调用instantiate生成beanInstance的Object放到包装类BeanWrapper中，返回BeanWrapper
	 */
	public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
			@Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

		// BeanWrapper是包装bean的容器
		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		// 确定的构造方法
		Constructor<?> constructorToUse = null;
		// 确定的构造方法的参数值，用于反射,ArgumentsHolder是个数据结构，他的arguments属性才是真实的属性值
		// 也就是下面的argsToUse
		ArgumentsHolder argsHolderToUse = null;
		// 确定的构造方法的参数的值
		Object[] argsToUse = null;

		// 确定参数值列表
		// argsToUse可以有两种办法设置
		// 第一种通过beanDefinition设置
		// 第二种通过xml设置
		if (explicitArgs != null) {
			argsToUse = explicitArgs;
		}
		else {
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				// 获取已解析的构造方法
				// 一般不会有，因为构造方法一般会提供一个
				// 除非有多个。那么才会存在已经解析完成的构造方法
				constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached constructor...
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			if (argsToResolve != null) {
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve);
			}
		}

		if (constructorToUse == null) {
			// 如果没有已经解析的构造方法
			// 则需要去解析构造方法
			// Need to resolve the constructor.
			// 判断构造方法是否为空，判断是否根据构造方法自动注入
			boolean autowiring = (chosenCtors != null ||
					mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			ConstructorArgumentValues resolvedValues = null;

			// 定义了最小参数个数
			// 如果你给构造方法的参数列表给定了具体的值
			// 那么这些值得个数就是构造方法参数的个数
			int minNrOfArgs;
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			}
			else {
				// 用来存放构造方法的参数值，存放了参数值和参数值所对应的下标
				ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
				// 用于存放解析后的构造函数参数的值
				resolvedValues = new ConstructorArgumentValues();
				// 解析配置文件中配置的构造方法的参数个数，表示后面要选择的构造函数的参数不能小于这个数
				minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
			}

			// Take specified constructors, if any.
			Constructor<?>[] candidates = chosenCtors;
			if (candidates == null) {
				Class<?> beanClass = mbd.getBeanClass();
				try {
					// 如果传入的候选构造方法列表为null，就通过反射获取全部构造方法
					candidates = (mbd.isNonPublicAccessAllowed() ?
							beanClass.getDeclaredConstructors() : beanClass.getConstructors());
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Resolution of declared constructors on bean Class [" + beanClass.getName() +
							"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
				}
			}

			/**
			 * 给构造方法列表排序
			 * 首先看访问权限，高的放前面
			 * 其次看参数个数，访问权限一样的时候，参数多的排前面
			 * 如下:
			 * public OrderService(AService aService, TaskService taskService, Object obj)
			 * public OrderService(AService aService, TaskService taskService)
			 * public OrderService(AService aService)
			 *
			 * protected OrderService(AService aService, TaskService taskService, Object obj, Integer i)
			 * protected OrderService(AService aService, TaskService taskService)
			 * protected OrderService(Object obj)
			 *
			 * OrderService(AService aService, Object obj)
			 *
			 * private OrderService(AService aService, TaskService taskService, Object obj)
			 */
			AutowireUtils.sortConstructors(candidates);
			// 定义了一个差异变量，默认是Integer最大值
			int minTypeDiffWeight = Integer.MAX_VALUE;
			/**
			 * 有歧义的构造方法
			 * 就是比如 有两个构造方法，一个接受User对象，另一个接受User类的父类
			 * 那我要注入User对象的话，这两个构造都能使用，这就是有歧义
			 */
			Set<Constructor<?>> ambiguousConstructors = null;
			LinkedList<UnsatisfiedDependencyException> causes = null;

			// 循环所有的构造方法
			for (Constructor<?> candidate : candidates) {
				Class<?>[] paramTypes = candidate.getParameterTypes();

				/**
				 * constructorToUse != null 表示Spring已经找到了一个构造方法，
				 * 但是Spring选择构造的方式是，优先权限高的，然后参数多的
				 * 所以argsToUse.length > paramTypes.length
				 * 就是来判断如果当前构造的参数都小于上次解析的构造了，就不需要再去找了
				 * 因为Spring在前面已经按规则排好序了，后面的都是参数更少的构造了
				 *
				 * 唯一有问题的就是访问权限一样，参数个数还一样的，那么Spring就要去做进一步处理
				 */
				if (constructorToUse != null && argsToUse.length > paramTypes.length) {
					// Already found greedy constructor that can be satisfied ->
					// do not look any further, there are only less greedy constructors left.
					// 已经找到可以满足的贪婪构造函数->不要再看下去了，只剩下更少贪婪的构造函数了
					break;
				}
				if (paramTypes.length < minNrOfArgs) {
					continue;
				}

				// 保存参数组合的私有内部类，用于后面计算权重
				ArgumentsHolder argsHolder;
				if (resolvedValues != null) {
					try {
						// 如果加了@ConstructorProperties注解，则把值取出来
						// @ConstructorProperties(value = {"xxx", "111"})
						String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
						if (paramNames == null) {
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								/**
								 * 获取构造方法参数名称列表
								 * 比如参数列表(AService aService, TaskService taskService)
								 * 将aService和taskService加入到数组
								 */
								paramNames = pnd.getParameterNames(candidate);
							}
						}
						// 根据paramNames和paramTypes将参数值创建出来 如果找不到bean，产生异常延后抛出异常
						argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
								getUserDeclaredConstructor(candidate), autowiring);
					}
					catch (UnsatisfiedDependencyException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
						}
						// Swallow and try next constructor.
						if (causes == null) {
							causes = new LinkedList<>();
						}
						causes.add(ex);
						continue;
					}
				}
				else {
					// Explicit arguments given -> arguments length must match exactly.
					if (paramTypes.length != explicitArgs.length) {
						continue;
					}
					argsHolder = new ArgumentsHolder(explicitArgs);
				}


				/**
				 * 计算类型差异量
				 * argsHolder.arguments和paramTypes之间的差异
				 * 每个参数值的类型与构造方法参数列表的类型直接的差异
				 * 通过这个差异量来衡量或者确定一个合适的构造方法
				 */
				int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
						argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
				// Choose this constructor if it represents the closest match.
				// 差异量越小，就选择当前构造
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = candidate;
					argsHolderToUse = argsHolder;
					argsToUse = argsHolder.arguments;
					// 将最小差异量设置为当前差异量
					minTypeDiffWeight = typeDiffWeight;
					// 清空相同差异值的构造列表 为什么? 看下面注释
					ambiguousConstructors = null;
				}
				/**
				 * 差异值相等的情况下，添加到ambiguousConstructors中，但不立即抛出异常
				 * 为什么不立刻抛出异常而是在for循环外抛出?
				 * 因为可能后面要循环的构造可能会更适合，那么Spring会在找到后将ambiguousConstructors清空
				 */
				else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
					if (ambiguousConstructors == null) {
						ambiguousConstructors = new LinkedHashSet<>();
						ambiguousConstructors.add(constructorToUse);
					}
					ambiguousConstructors.add(candidate);
				}
			}

			// 循环结束没有找打合适的构造方法则抛出异常
			if (constructorToUse == null) {
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Could not resolve matching constructor " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
			}
			// 如果ambiguousConstructors还存在则异常？为什么会在上面方法中直接exception？
			// 上面注释当中有说明
			else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Ambiguous constructor matches found in bean '" + beanName + "' " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
						ambiguousConstructors);
			}

			if (explicitArgs == null) {
				/*
				 * 缓存相关信息，比如：
				 *   1. 已解析出的构造方法对象 resolvedConstructorOrFactoryMethod
				 *   2. 构造方法参数列表是否已解析标志 constructorArgumentsResolved
				 *   3. 参数值列表 resolvedConstructorArguments 或 preparedConstructorArguments
				 *   这些信息可用在其他地方，用于进行快捷判断
				 */
				argsHolderToUse.storeCache(mbd, constructorToUse);
			}
		}

		try {
			/*
			 * 使用反射创建实例 lookup-method 通过CGLIB增强bean实例
			 */
			final InstantiationStrategy strategy = beanFactory.getInstantiationStrategy();
			Object beanInstance;

			if (System.getSecurityManager() != null) {
				final Constructor<?> ctorToUse = constructorToUse;
				final Object[] argumentsToUse = argsToUse;
				beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
						strategy.instantiate(mbd, beanName, beanFactory, ctorToUse, argumentsToUse),
						beanFactory.getAccessControlContext());
			}
			else {
               
				beanInstance = strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse);
			}

			bw.setBeanInstance(beanInstance);
			return bw;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean instantiation via constructor failed", ex);
		}
	}
```

逻辑实在太复杂，挑重要的说:

1.创建BeanWrapperImpl，用于包装bean

2.解析配置文件中配置的构造函数参数列表，得出minNrOfArgs，即需要的构造函数的最小参数个数，没有配置就是0

3.如果传入的候选构造方法列表为null，就直接通过反射获取所有的构造方法，默认Spring是允许访问非public的构造的

4.将候选构造函数列表排序，先按访问权限从大到小排，访问权限一样的话，参数从多到少排

5.循环所有的构造方法

​	5.1如果已结找到了一个暂时确定的构造函数，并且当前构造函数的参数个数 小于 最小暂时确定的构造函数的参		数个数就不需要再找了，因为是排好序的

​	5.2如果当前构造函数的参数列表 小于 minNrOfArgs，就直接循环下次，因为可能访问权限小的构造函数的参		数个数满足minNrOfArgs

​	....中间过程省略，不重要(不重要的意思是和Spring到底怎么选择构造方法的思路不是强关联)

​	5.3 计算类型差异量，差异量越小，就选择当前构造，若差异量和之前的最小差异量一样，就加到一个集合中，**但不立即抛出异常，因为可能后面会有更合适的**，找到后会清空这个集合来消除歧义

6.循环结束后没找到就抛异常，如果存在有歧义的构造方法，且是严格模式来解析构造函数则抛出异常，默认是宽松模式

7.最后去创建实例 ，如果传入的参数为null，则通过无参构造实例化。

​	**如果存在有歧义的构造方法，又是宽松模式，会通过最终确定的那个构造函数来创建实例。**

​	**具体什么意思看5.12.3**



**总结：**

**1.创建 BeanWrapperImpl 对象**

**2.构造候选方法只有一个的情况，满足就构造**

**3.构造候选方法多个的情况，获取构造方法和参数列表，并排序**

**4.排序规则是，优先public和参数多的**

**5.如果有个public而且参数多于需要的参数，选之**

**6.其次，选择参数相等的，参数不足的直接忽略**

**7.参数相等的情况，做类型转化，计算一个typeDiffWeight，相似参数的度量，选择最相似的，如果多个typeDiffWeight相等且是严格解析构造模式，那么报错。**

**8.最后调用instantiate生成beanInstance的Object放到包装类BeanWrapper中，返回BeanWrapper**





####5.12.3 推断构造时Spring为什么是延后处理异常? 产生歧义后为什么Spring还是构造出实例了?

 解析构造函数的时候，会去从容器中取出对应的bean，如果getBean获取不到就出异常了，但是也不立即派出，因为可能后面还会有更合适的。



还有产生歧义的时候也是延后处理异常，因为首先我们要知道，Spring在选择最合适的构造方法的时候，会将传入进来的候选构造列表进行排序，但是排序只会根据构造方法的访问修饰符和参数个数来进行排序，并不会根据类型来排序，因此，若多个构造的权限一样，且参数列表一样，其排序后的结果对于Spring来说并不是确定的，那么实际上有可能最合适的方法还在后面的。找到后再把有歧义的构造列表清除，因此Spring选择了延后处理异常。

什么情况下会造成歧义? 就是重载的时候访问权限、参数列表类型、长度一样的时候

```java
@Autowired(required = false)
public OrderServiceImpl(AService aService, TaskService taskService) {
    this.aService = aService;
    this.taskService = taskService;
}

@Autowired(required = false)
public OrderServiceImpl(TaskService taskService, AService aService) {
    this.aService = aService;
    this.taskService = taskService;
}
```



但是如果确实是只有存在歧义的构造函数是最合适的，那么Spring会判断**是否是宽松的解析构造模式**，默认是宽松的，那么也不会抛出异常。

```
else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) 
```

那么Spring接下来怎么做的呢? 为什么产生歧义后Spring还是构造出实例了呢?

因为当Spring判断为是宽松解析构造模式后，那么他就是通过产生了歧义的构造方法和参数通过反射去创建实例的，比如**当遍历到构造A时确定其是最合适的构造(赋值给constructorToUse变量)，然后遍历到构造B，发现构造A和B产生歧义，此时constructorToUse还是为构造A，如果后续还没有更合适的构造来消除歧义的话，那么Spring就通过构造A来进行实例化**。

```java
beanInstance = strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse);
```



可能我文字这么描述大家还是很混乱，那么大家可以根据下面的例子，多debug几遍就能理解了

```java
@Service("orderService")
public class OrderServiceImpl implements OrderService {

	private TaskService taskService;

	private AService aService;

	public OrderServiceImpl() {}

	@Autowired(required = false)
	public OrderServiceImpl(AService aService, TaskService taskService) {
		this.aService = aService;
		this.taskService = taskService;
	}

	@Autowired(required = false)
	public OrderServiceImpl(String s, Integer i) {
	}

	@Autowired(required = false)
	public OrderServiceImpl(Integer i, String s) {
	}

	@Autowired(required = false)
	public OrderServiceImpl(TaskService taskService, AService aService) {
		this.aService = aService;
		this.taskService = taskService;
	}

	@Autowired(required = false)
	public OrderServiceImpl(TaskService taskService) {
		this.taskService = taskService;
	}

	@Override
	public void query() {
		System.out.println("order query");
	}

}

```





======================================我是分隔符(*^__^*) ====================================



##五. Spring是如何解决循环依赖的?

![1586535594085](C:\Users\49016\AppData\Roaming\Typora\typora-user-images\1586535594085.png)

假如A依赖了B，B也依赖了A

那么假设首先初始化A，



**第一步，就是初始化了A，但此时的A并没有被填充属性**

getBean("A")

​	getSingleton("A") -- 第一次初始化A直接返回null

​	mbd.isSingleton()

​		singletonsCurrentlyInCreation.add 添加到singletonsCurrentlyInCreation集合标记为正在创建

​		AbstractAutowireCapableBeanFactory类的createBean() -- 去创建实例

​		singletonsCurrentlyInCreation.remove(beanName) -- 将正在创建的标记清除

​		singletonObjects.put(beanName, singletonObject) -- put到存放已经实例化完成的map中

​	最后从容器中获取实例化好后的bean，return	



createBean() --A流程

​	doCreateBean()

​		createBeanInstance() --真正创建实例

​		addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));

​		为了防止循环引用，尽早持有对象的引用

```java
this.singletonFactories.put(beanName, singletonFactory);   this.earlySingletonObjects.remove(beanName);
this.registeredSingletons.add(beanName);
```

​		populateBean -- 填充属性

​		initializeBean -- 执行后置处理器



populateBean() -- A填充属性流程

​	AutowiredAnnotaionBeanPostProcessor==>postProcessPropertyValues==>inject()

​		element.inject() -- 遍历属性去注入

​			resolveDependency() -- 根据容器中Bean定义，解析指定的依赖关系，获取依赖对象

​				doResolveDependency()

​					getBean("B") -- 为了注入B，此时就要去容器中获取B



**第二步，接下来就到了A要去找他依赖的B了，但B还没有，此时先初始化B**

getBean("B")

​	getSingleton("B")  -- 第一次初始化B直接返回null

​	...

​	doCreateBean()

​		addSingletonFactory()

```java
this.singletonFactories.put(beanName, singletonFactory);
this.earlySingletonObjects.remove(beanName);
this.registeredSingletons.add(beanName);
```

​		populateBean() -- 给B填充属性

​			AutowiredAnnotaionBeanPostProcessor==>postProcessPropertyValues==>inject()

​				element.inject()

​					resolveCandidate()

​						getBean("A")



**第三步，B初始化完后，获取前面初始化好的不完全的A，填充给B**

getBean("A")

​	getSingleton("A")

​		1.从singletonFactories这个map中获取不完全的A

​		2.将不完全的A放入earlySingletonObjects这个map，用于后面的校验

​		3.从singletonFactories移除A

​	getObjectForBeanInstance() -- 获取A对象的实例

​	return bean

这个时候就回到了上面**第二步**里面的resolveCandidate()

instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);

然后调用栈又回到element.inject()，做一些校验，

field.set(bean, value); 通过JDK反射将不完全的A注入给B，此时B称为完全的B



然后回到**第二步**里的doCreateBean()

此时populateBean()执行完成，

然后开始执行B对象的后置处理器，

注册销毁逻辑，

去掉正在创建的标识，

将B放入到singletonObjects中，

从singletonFactories和earlySingletonObjects中移除B，至此B的实例化完成

```java
this.singletonObjects.put(beanName, singletonObject);
this.singletonFactories.remove(beanName);
this.earlySingletonObjects.remove(beanName);
this.registeredSingletons.add(beanName);
```



**第四步，将完整的B注入给A，完成循环依赖**

回到第一步里的element.inject()

value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);

此时的value是B的实例对象

然后通过JDK反射将完整的B注入给A，**此时A也变得完整**

然后执行A的后置处理器

注册销毁逻辑，

去掉正在创建的标识，

将A放入到singletonObjects中，

从singletonFactories和earlySingletonObjects中移除A，**至此A的实例化完成**



**关于3个map和一个set的说明：**

一级缓存：singletonObjects：用于存放完全初始化好的 bean，从该缓存中取出的 bean 可以直接使用
二级缓存：earlySingletonObjects：提前曝光的单例对象的cache，存放原始的 bean 对象（尚未填充属性），用于解决循环依赖
三级缓存：singletonFactories：单例对象工厂的cache，存放 bean 工厂对象，用于解决循环依赖

也就是说Spring通过了两个map singletonFactories和earlySingletonObjects来帮助实现循环依赖，

singletonFactories用于存放刚实例化但没有填充属性的对象，

earlySingletonObjects是用于从singletonFactories里取出要注入的对象后做类型校验的，

**singletonFactories和earlySingletonObjects是互斥的，**

当B来填充属性获取A时，就从singletonFactories取出对象，加入到earlySingletonObjects，然后从singletonFactories移除。

最后实例化好后的对象放入到singletonObjects和registeredSingletons中，并从singletonFactories和earlySingletonObjects中移除。



registeredSingletons是按顺序存放已经注册的SingletonBean的名称



https://blog.csdn.net/github_38687585/article/details/82317674

https://blog.csdn.net/f641385712/article/details/92801300

从字面意思来说：singletonObjects指单例对象的cache，singletonFactories指单例对象工厂的cache，earlySingletonObjects指提前曝光的单例对象的cache。以上三个cache构成了三级缓存，Spring就用这三级缓存巧妙的解决了循环依赖问题。



分析getSingleton的整个过程，Spring首先从singletonObjects（一级缓存）中尝试获取，如果获取不到并且对象在创建中，则尝试从earlySingletonObjects(二级缓存)中获取，如果还是获取不到并且允许从singletonFactories通过getObject获取，则通过singletonFactory.getObject()(三级缓存)获取。如果获取到了则将singletonObject放入到earlySingletonObjects,也就是 将三级缓存提升到二级缓存中！

1. 先从一级缓存singletonObjects中去获取。（如果获取到就直接return）

2. 如果获取不到或者对象正在创建中（isSingletonCurrentlyInCreation()），那就再从二级缓存earlySingletonObjects中获取。（如果获取到就直接return）

3. 如果还是获取不到，且允许singletonFactories（allowEarlyReference=true）通过getObject()获取。就从三级缓存singletonFactory.getObject()获取。（如果获取到了就从singletonFactories中移除，并且放进earlySingletonObjects。其实也就是从三级缓存移动（是剪切、不是复制哦~）到了二级缓存）

   

   

   

**最后说明下，对于构造函数的循环依赖 和 原型的循环依赖，Spring并不支持， 因为加入singletonFactories三级缓存的前提是执行了构造器，所以构造器的循环依赖没法解决**



​	

======================================我是分隔符(*^__^*) ====================================





## 六. Bean的生命周期与Bean的后置处理器

https://www.cnblogs.com/zzq6032010/p/11789076.html

1.Spring在refresh()方法中初始化完工厂后，会去执行所有BeanFactoryPostProcessors，其中比较重要的就是解析配置文件，将我们需要交给IOC管理的类转成BeanDefinition放入到工厂里的一个map中

2.然后接下来会注册和实例化bean的后置处理器，再开始实例化我们非懒加载的bean

3.在实例化bean的过程中，Spring**会执行9次，共5种接口的**bean的后置处理器，Bean的后置处理器贯穿了bean的整个实例化以及初始化过程



**调用BeanPostProcessor==>**

### 第一次，是在调用doCreateBean()之前

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean#resolveBeforeInstantiation

```
InstantiationAwareBeanPostProcessor
	postProcessBeforeInstantiation
实现了这个接口的处理器，会在bean实例化之前，执行postProcessBeforeInstantiation，
用于判断该bean要不要产生一些新的对象，这个方法可以返回任意类型。如果返回null，就会继续走Spring后面的代码。
如果返回的不是null，那么就不会再执行后面的逻辑，
只会去执行其父类的postProcessAfterInitialization()方法。

经典的使用场景就是aop，用于找出不能被代理的bean，其实就是切面类，配置类(配置类不是在这里被AOP代理的)
那么就会把这个类放入到一个map中，在后续完成aop增强的时候，就会忽略这个map里的bean。
判断方式就是判断几个AspectJ的注解，至于其他的目前就不能确定，
因此要等到后面执行postProcessAfterInitialization()方法解析表达式后才能来决定。
protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}
	
@EnableAspectJAutoProxy注解向spring注册了一个bean AnnotationAwareAspectJAutoProxyCreator.class
```



### 第二次，是在推断构造方法的时候，通过后置处理器来选出构造方法的候选列表 

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance#determineConstructorsFromBeanPostProcessors

```
SmartInstantiationAwareBeanPostProcessor ==> 主要是AutowiredAnnotationBeanPostProcessor
	determineCandidateConstructors
```



### 第三次，是实例化bean后

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean#applyMergedBeanDefinitionPostProcessors

```
MergedBeanDefinitionPostProcessor
	postProcessMergedBeanDefinition
缓存bean的注入信息的后置处理器，仅仅是缓存或者干脆叫做查找更加合适，没有完成注入，
注入是另外一个后置处理器的作用
```



### 第四次，为了处理循环依赖，尽早持有对象的引用

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean#getEarlyBeanReference

```
SmartInstantiationAwareBeanPostProcessor
	getEarlyBeanReference
循环引用的后置处理器，这个东西比较复杂， 获得提前暴露的bean引用。
主要用于解决循环引用的问题，只有单例对象才会调用此方法。
```



### 第五次，是在准备填充属性的时候

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean

```
InstantiationAwareBeanPostProcessor
	postProcessAfterInstantiation
返回布尔类型，用于判断是不是需要属性填充，如果返回false则直接从populateBean中返回，不再执行第6/7/8处。
```



### 第六次，是在正式去填充属性的时候

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean

```
InstantiationAwareBeanPostProcessor
	postProcessPropertyValues
此处用于做属性填充	
```



### 第7次，是在bean完成依赖注入后，执行@PostConstruct

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)

```
执行后置处理的befor
BeanPostProcessor
	postProcessBeforeInitialization
```



### 第8次，也在bean完成依赖注入后，执行完before之后

```
执行后置处理的after
BeanPostProcessor
	postProcessAfterInitialization
	
调用的是通常意义上BeanPostProcessor的after方法
Spring的切面就是基于此方法进行的，调用的实现方法是AbstractAutoProxyCreator#postProcessAfterInitialization。它会先将切面类放入advisedBeans中，标记为true，表示需要用切面拦截。然后调用AbstractAutoProxyCreator#createProxy方法生成代理。	
```



### 第9次，是在bean被销毁的时候

 第九处是在执行AbstractApplicationContext#close方法销毁bean时触发的，最终调用到的是DisposableBeanAdapter#destroy，在此方法中调用了：DestructionAwareBeanPostProcessor.postProcessBeforeDestruction。用于在销毁bean之前做操作。为什么DestructionAwareBeanPostProcessor中没有after方法？因为执行after的时候所有bean都没了，Spring认为你也没必要做什么扩展了。