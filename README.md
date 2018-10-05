# Java区块链（单机版）
---
## 介绍
&emsp;&emsp;这个是scnu的一个Java作业，要求是编写一个简单的区块链系统，实现转账交易、挖矿、计算余额。这个项目（其实并不算项目）是按照我自己的理解写出来的区块链模型，其中包括：
### 结构
* 区块(BLock)：存储了索引号、上一个区块的哈希、时间戳、交易信息、幸运数字（挖矿时产生的）、当前区块的哈希。
* 区块链(BlockChain)：相当于存储区块的一个链表。
* 交易信息(Transaction)：就是放进Block的交易信息，存储了交易发起者、交易接收者、交易金额、交易附加信息(data)。
* 账户(Account)：存储了账户的信息，包括账户名、私钥（建账户的时候随机生成）、公钥（由私钥生成）、地址（由公钥生成，但暂时没有用到）、余额、账户密码。（Ps:账户是自己觉得应该有所以才加上去的，查资料的时候并没有查到相关的东西）
* 账户索引(accounts)：根据账户的名字得到这个账户，利用Map。
### 方法
* hash256(...)：传入字符串返回对应的sha256值。（找来的方法，代码上有来源）
* encrypt(...)和decrypt(...)：通过RSA对数据进行加密（公钥）和解密（私钥）。（找来的方法，代码上有来源）
* sign(...)和verify(...)：通过RSA对数据进行签名（私钥）和认证（公钥）。（找来的方法，代码上有来源）
* Account.refreshBalance(...)：通过遍历主账本更新账户的余额（可以理解为余额核对）。
* Account.createTransaction(...)：创建一笔交易，创建成功返回true，否则返回false。
* Account.getTransaction(...)：对一笔交易进行签名认证，认证通过返回true，失败返回false。
* createGenesisBlock()：创建创世区块（第一个），同时创建系统账户。
* mineBlock(...)：挖矿，其实是把区块加入区块链的过程，因为区块太容易（或太快）被加入区块链（主账本）会有安全问题，所以要增加一些难度，比如改变幸运数字（nonce）知道该区块的哈希值前几位是0为止。（个人理解。。。）
* createTransaction(...)：创建交易，对交易信息列表进行一一验证，然后交给矿工处理。（单机版省略了找矿工的过程，好像要广播？）
* checkChain(...)：通过检测区块前后哈希检验链的合法性。
### 过程
&emsp;&emsp;在初始化的时候，先创建创世区块和系统账户（防止系统账户被抢先注册），然后创建若干测试用户。
&emsp;&emsp;在交易时，首先收集若干交易（用一个Transaction的List），收集到一定程度的时候创建交易（createTransaction），对每一条交易进行验证并刷新账户余额（Account.createTransaction和Account.getTransaction），验证是首先支付方用收钱方的公钥对数据（这里是交易金额）进行加密，同时用自己的私钥对数据进行签名，然后传给收钱方，收钱方收到后用自己的私钥进行解密，同时用支付方的公钥对数据进行验证。在对每一条数据验证完后，把验证成功的数据递交给矿工，矿工在交易数据的基础上增加系统对自己的挖矿奖励（因为后面生成哈希值的时候只能改变nonce所以先加进去），成功算出哈希后就生成Block并加入主账本。
&emsp;&emsp;在查询余额时因为每次交易都会刷新余额，所以可以直接 Account.getBalance()，（不过这个是我认为每次都要遍历的话有点耗时才这样做的，不代表主流做法），也可以通过遍历主账本，利用收入减支出的方法计算余额（Account.refreshBalance）。（在查询余额时我设置了要加密码。。不过信息都是公开的就。。x_x）
## 参考资料
&emsp;&emsp;这里贴一些我查到的比较有用的资料：
- 官网：
	- [BLOCKCHAIN](https://www.blockchain.com/btc/tx)
- 参考的成品（这个是我们老师给的，可以看javascript源码喔）：
	- [https://anders.com/blockchain/coinbase.html](https://anders.com/blockchain/coinbase.html)
- 一些原理的介绍：
	- [知乎](https://www.zhihu.com/question/20792042)
- 参考的代码：
	- [一起来编写最简单的区块链](https://www.leiphone.com/news/201808/hItR5xBCgTC0kT1l.html)
	- [Java实现一个简单的比特币系统](https://my.oschina.net/u/3796575/blog/1791185)
- 公钥、私钥、RSA在区块链的用法：
	- [比特币『私钥』『公钥』『钱包地址』间的关系](https://blog.csdn.net/pony_maggie/article/details/54837674)
	- [公钥、私钥、钱包都是什么——小白区块链实战指南(二)](https://www.jianshu.com/p/174bb88d969d)
	- [gist上的RSA代码](https://gist.github.com/LuisMichaelis/53c40a1681607e758d4e65b85f210117)
