# TSBridgeKotlin

目的：Android 学习过程中开发一个简单的小应用

语言：使用 Kotlin 语言编写（原先为 Java ）

功能实现：
1、两个页面，分别用于公告发布与显示；

2、发布页面包含发布者名称、发布文本内容输入文本框，发布图片内容选择、清除按钮，发布按钮；
内容分文字和图片两部分，只要有其一即可发布，清除按钮目前只是用来清空已选图片；
注：发布信息存入 Bmob 云数据平台

3、显示页面以列表的形式将公告内容进行展示，包括发布者头像、名称、内容（文字或图片），其中图片内容可以点击放大、还原；

4、点击公告图片内容会以弹出的对话框放大显示，对话框内容是自定义布局（用 Anko 直接在 Kotlin 代码中编写，不再借助 xml）；

5、添加了需要联网操作的网络连接判断，如果没有连接则给出页面让用户点击按钮去设置网络（包括移动和无线两个入口）；

6、添加了 SDK 6.0 及以上版本设备的危险权限申请机制，目前有 Manifest.permission.WRITE_EXTERNAL_STORAGE；
当用户拒绝并选择了不再显示时，下次操作需要该权限时会显示页面让用户点击按钮去设置权限；
注：同上面的网络设置，从设置页面回到某页面时会自行判断所需条件是否满足，如果满足则进行下一步，否则给出提示

7、添加了公告信息列表的下拉刷新功能，如果有最新的数据则加载
注：最新的数据显示在列表上方，即开头部分

8、当activity的上下文context销毁以后，若 Glide 的 with 和其发生关联正在异步加载图片，那么就会出现异常，
解决方法是开始新建加载任务时就通过 context 去获取全局的上下文（ onetxt.getApplicationContext() ），
在 Kotlin 中是 context.applicationContext；

9、将原先利用异步类 + Glide 实现加载图片改为了 Glide，因为其本身就是异步的，而图片加载完成后会自动切换到主线程去显示图片；

10、添加了用户信息，包括注册、登录、注销，
只有登录用户才能才送消息，而接收则没有此要求；

11、消息的用户头像部分设置成圆形；

12、Utils 中 Toast 显示放置在 Handler 中执行，
也可以直接利用 Kotlin-Anko 提供的 toast()，但是都需要 Context 上下文环境；

13、BulletinFragment 读取查询到的 Bulletin 信息时由 for 改成了 map实现；

14、Utils setImageToView 方法在没有网络的情况下会异常，
query.findObjects(object : FindListener<User>(){...}) 不接收 null 参数，
所以在准备加载图片之前需要进行网络判断；

15、更正命名空格部分，变量定义时前无后一，类继承时冒号前后均一；

16、Class<T> class 作为参数时，在 Java 中为 ClassName.class，而在 Kotlin 中是 ClassName::class.java，
在 LoginFragmnet 中获取 User 信息时用到了，var user = BmobUser.getCurrentUser(User::class.java)；

17、暂时将工具栏去除，等以后需要在上面添加功能按键时再加回，
相应地，将各种配色改为浅灰；

18、将 Tablayout Text 字体由默认改为 20dp，与标题及按钮一致；