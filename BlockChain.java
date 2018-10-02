import java.util.*;
import java.security.*;
import java.math.*;
import java.security.spec.ECGenParameterSpec;
import javax.crypto.Cipher;
import sun.security.ec.ECPublicKeyImpl;
import static java.nio.charset.StandardCharsets.UTF_8;


public class BlockChain{
    /*sha加密*/
    /*https://gist.github.com/avilches/750151*/
    public static String hash256(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes());
        return bytesToHex(md.digest());
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    /*RSA*/
    /*https://gist.github.com/dmydlarz/32c58f537bb7e0ab9ebf*/
    public static KeyPair buildKeyPair() throws NoSuchAlgorithmException {
        final int keySize = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.genKeyPair();
    }
    public static byte[] encrypt(PublicKey publicKey, String message) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(message.getBytes());
    }
    public static byte[] decrypt(PrivateKey privateKey, byte [] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(encrypted);
    }

    /*https://gist.github.com/LuisMichaelis/53c40a1681607e758d4e65b85f210117*/
    public static byte[] sign(PrivateKey key, byte[] data) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(data);

            return signature.sign();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unexpected: No RSA algorithm found!", e);
        } catch (SignatureException | InvalidKeyException e) {
            throw new RuntimeException("Error signing some data!", e);
        }
    }
    public static String sign(PrivateKey key, String data) {
        return Base64.getEncoder().encodeToString(sign(key, data.getBytes(UTF_8)));
    }
    public static boolean verify(PublicKey key, byte[] data, byte[] sig) {  //data就是sign的data!
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update(data);

            return signature.verify(sig);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unexpected: No RSA algorithm found!", e);
        } catch (SignatureException | InvalidKeyException e) {
            throw new RuntimeException("Error verifying some data!", e);
        }
    }
    public static boolean verify(PublicKey key, String data, String sig) {
        return verify(key, data.getBytes(UTF_8), Base64.getDecoder().decode(sig));
    }

    /*区块*/
    public static class Block{
        private int index;
        private String preHash;
        private long timeStamp;
        private List<Transaction> coinBase;
        //private String data;  //test...
        private int nonce;
        private String hash;
        public Block(int id,String ph,long ts,List<Transaction> cb,int no){
            try{
                this.index = id;
                this.preHash = ph;
                this.timeStamp = (new Date()).getTime();
                //this.data = dt;
                this.coinBase = cb;
                this.nonce = no;
                this.hash = hash256(this.index+this.preHash+this.coinBase+this.nonce);  //不要timestamp?
            }
            catch(Exception e){
                System.out.println("Wrong!");
            }
        }
        public int getIndex(){
            return this.index;
        }
        public String getPreHash(){
            return this.preHash;
        }
        public long getTimeStamp(){
            return this.timeStamp;
        }
        //public String getData(){ //test...
        //    return this.data;
        //}
        public List<Transaction> getCoinBase(){
            return this.coinBase;
        }
        public int getNonce(){
            return this.nonce;
        }
        public String getHash(){
            return this.hash;
        }
        public String toString(){
            String s = "\n\n"
                    + "Index:\t" + Integer.toString(this.index) + '\n'
                    + "PreHash:\t" + this.preHash + '\n'
                    + "TimeStamp:\t" + Long.toString(this.timeStamp) + '\n'
                    + "Data:\n" ;
            if(this.coinBase != null){
                for(Transaction i : this.coinBase){
                    s += "\t\t" + i.toString() + '\n';
                }
            }
            else{
                s += "\t\tnull!\n";
            }
            s += "Nonce:\t\t" + Integer.toString(this.nonce) + '\n'
                    + "Hash:\t\t" + this.hash + "\n\n";
            return s;
        }
    }

    /*链头？*/
    static List<Block> blockChain = new ArrayList<Block>();

    /*账户*/
    public static class Account{
        private String name;
        private PrivateKey priKey; //私钥@重要！
        private PublicKey pubKey;
        private String address; //暂时。。还是不用吧。。。
        private double balance;
        private String passwd; //账户密码？手动加上去的，非必要。
        public Account(String na,String pw){
            try{
                this.name = na;
                this.passwd = pw;
                this.balance = 0;
                if(this.name == "__@SYSTEM__") this.balance = Double.POSITIVE_INFINITY; //系统，来源
                KeyPair keyPair = buildKeyPair();
                // 获取公钥
                this.pubKey = keyPair.getPublic();
                ////String puk = bytesToHex(publicKey.getEncoded()); //publicKey改了this.pubKey
                ////this.pubKey = puk;  //不是存字符串！
                // 获取私钥
                this.priKey = keyPair.getPrivate();
                ////String prk = bytesToHex(privateKey.getEncoded()); //privateKey改了this.priKey
                ////this.priKey = prk;  //不是存字符串！
                //找不到RipeMD160 跳过。。。
                String puh = hash256(hash256(new String(this.pubKey.getEncoded())));  //公钥哈希
                puh = puh.substring(0,8);  //取头四字节

                String bs64 = Base64.getEncoder().encodeToString(puh.getBytes("utf-8")); //地址暂时不用吧（因为不会&&复杂- -）
                this.address = bs64;
                //byte[] asBytes = Base64.getDecoder().decode(asB64);  //base64解码
            }
            catch(Exception e){
                System.out.println(e);
            }
        }
        public PublicKey getPublicKey(){
            return this.pubKey;
        }
        public String getName(){
            return this.name;
        }
        public String decode(String encrypted){
            String result = new String();
            try{
                result = new String(decrypt(this.priKey,encrypted.getBytes(UTF_8)));
            }
            catch(Exception e){
                System.out.println(e);
            }
            return result;
        }
        public String signData(String data){
            String result = new String();
            try{
                result = sign(this.priKey,data);
            }
            catch(Exception e){
                System.out.println(e);
            }
            return result;
        }
        public double getBalance(String pw){
            if(pw == this.passwd) return this.balance;
            else{
                System.out.println("密码错误！(Password not correct!)");
                return -1.0;
            }
        }
        public void refreshBalance(List<Block> bc){ //传入区块链（虽然单机版没什么用。。。）
            if(this.name == "__@SYSTEM__"){
                return; //SYSTEM不能刷新
            }
            double bl = 0; //初始值为0
            for(Block block : bc){
                if(block.getCoinBase() == null) continue; //针对创世
                for(Transaction tx : block.getCoinBase()){
                    if(tx.getFromUser() == this) bl -= tx.getAmount();
                    if(tx.getToUser() == this) bl += tx.getAmount();
                }
            }
            if(Math.abs(this.balance-bl) < 0.0000001) System.out.println("余额不须要改变！(Balance unchanged!)");
            else{
                this.balance = bl;
                System.out.println("余额已刷新！(Balance refreshed!)");
            }
        }
        public boolean createTransaction(Account res,double amount){  //发送，创建（检验）交易
            if(amount > this.balance) return false; //不够钱
            if(amount <= 0.0) return false; //要正数
            //if(res == this) return false; //别转给自己啊。。。
            boolean result = false;
            try{
                byte[] enc = encrypt(res.getPublicKey(),Double.toString(amount)); //加密
                byte[] sig = sign(this.priKey,Double.toString(amount).getBytes(UTF_8)); //签名
                /*此处省略传输过程*/
                result = res.getTransaction(this,enc,sig);
                if(result) this.balance -= amount; //扣钱
            }
            catch(Exception e){
                System.out.println(e);
            }
            return result;
        }
        public boolean getTransaction(Account sen,byte[] enc,byte[] sig){  //接受（检查）交易
            boolean check = false;
            try{
                byte[] data = decrypt(this.priKey,enc);  //解密
                check = verify(sen.getPublicKey(),data,sig);  //验证签名
                if(check) this.balance += Double.valueOf(new String(data));  //加钱
            }
            catch(Exception e){
                System.out.println(e);
            }
            return check;
        }
    }
    static Map<String,Account> accounts = new HashMap<String,Account>();  //存账户的，通过账户名寻找账户

    /*创建账户*/
    public static boolean createAccount(String name,String passwd){  //创建（注册）账户，输入账户名和账户密码
        if(accounts.get(name) != null){
            System.out.println("账户 " + name + " 已存在！(Account " + name + " existed!)");
            return false;
        }
        Account acc = new Account(name,passwd);
        accounts.put(name,acc);
        return true;
    }

    /*交易数据*/
    public static class Transaction{
        private Account fromUser;  //转账方
        private Account toUser;    //接收方
        private double amount;     //转账数目
        private String data = "";  //附加信息
        public Transaction(Account fU,Account tU,double am,String dt){
            this.fromUser = fU;
            this.toUser = tU;
            this.amount = am;
            this.data = dt;
        }
        public Account getFromUser(){
            return this.fromUser;
        }
        public Account getToUser(){
            return this.toUser;
        }
        public double getAmount(){
            return this.amount;
        }
        public String getData(){
            return this.data;
        }
        public String toString(){
            return "from: " + this.fromUser.getName() + " , to: " + this.toUser.getName() + " , amount: " + this.amount + " , data: \"" + this.data + "\"";
        }
    }


    /*创世区块*/
    public static void createGenesisBlock() throws Exception {
        int nonce = 1; //挖矿次数，幸运数字
        //createAccount("__@SYSTEM__","admin");
        Account sys = new Account("__@SYSTEM__","admin");  //系统账户
        accounts.put("__@SYSTEM__",sys);
        List<Transaction> cb = new ArrayList<Transaction>();
        String preHash = "0000" + hash256(Long.toString((new Date()).getTime())).substring(0,60);  //随机的哈希，其实感觉不随机也没问题
        while(true){
            @SuppressWarnings("unchecked")
            String hash = hash256(0+preHash+null+nonce);
            if (hash.startsWith("0000")) {
                System.out.println("=====计算结果正确，计算次数为：" +nonce+ ",hash:" + hash);
                break;
            }
            nonce++;
            //System.out.println("计算错误，hash:" + hash);
        }
        Block newBlock = new Block(0,preHash,(new Date()).getTime(),null, nonce);
        blockChain.add(newBlock);
        System.out.println("创世区块：\n" + newBlock.toString());
    }

    /*挖矿*/
    public static void mineBlock(List<Transaction> cb,Account miner) throws Exception{ //挖矿（重头戏！）
        Block latestBlock = blockChain.get(blockChain.size()-1);     //上一个区块
        if(accounts.get("__@SYSTEM__").createTransaction(miner,10)){ //奖励由系统给出
            cb.add(new Transaction(accounts.get("__@SYSTEM__"),miner,10,"挖矿奖励(Mining reward)")); //给自己的挖矿奖励,一次10个（此处有省略了多个矿工同时挖矿。。。）
        }
        else{
            System.out.println("签名认证不通过，奖励失败！(Sign not mached,reward failed!)"); //好像直接没奖励太不人道了。。。可以考虑给个重试的机会
        }
        int nonce = 1;
        //String hash = "";
        while(true){
            @SuppressWarnings("unchecked")
            String hash = hash256((latestBlock.getIndex()+1)+latestBlock.getHash()+cb+nonce);  //其实就是更改幸运数字，直到前几（4）位都为0
            if (hash.startsWith("0000")) {
                System.out.println("=====计算结果正确，计算次数为：" +nonce+ ",hash:" + hash);
                break;
            }
            nonce++;
            //System.out.println("计算错误，hash:" + hash);
        }
        Block newBlock = new Block(latestBlock.getIndex()+1,latestBlock.getHash(),(new Date()).getTime(),cb, nonce);
        blockChain.add(newBlock);  //挖成功了，加到链
        System.out.println("新增区块：\n" + newBlock.toString());
    }

    /*转账*/
    public static void createTransaction(List<Transaction> cb,Account miner){ //传入交易数据和矿工帐号(此处省略了找矿工的过程。。。)
        List<Transaction> newBlockList = new ArrayList<Transaction>();  //对传入交易链的每一个交易进行检验（顺便在检验时刷新余额）
        for(Transaction tx : cb){
            if(tx.getFromUser() == tx.getToUser()){
                System.out.println("不能转账给自己！(The same account!)");
                continue;
            }
            if(tx.getFromUser().createTransaction(tx.getToUser(),tx.getAmount())){  //检验成功
                newBlockList.add(tx);
            }
            else{
                System.out.println("签名认证不通过！(Sign not mached!):\n\t" + tx.toString());  //检验失败
            }
        }
        try{
            mineBlock(newBlockList,miner);  //检验完就挖矿呗
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    /*模拟黑客攻击*/

    public static void main(String[] args){
        try{
            createAccount("Tover","abc");
            createAccount("KunLin","aaa");

            createGenesisBlock();

            List<Transaction> cb1 = new ArrayList<Transaction>();
            cb1.add(new Transaction(accounts.get("__@SYSTEM__"),accounts.get("Tover"),10,"Hello KunLin!"));
            cb1.add(new Transaction(accounts.get("Tover"),accounts.get("KunLin"),5,"Hi Tover!"));
            createTransaction(cb1,accounts.get("Tover"));

            List<Transaction> cb2 = new ArrayList<Transaction>();
            cb2.add(new Transaction(accounts.get("Tover"),accounts.get("Tover"),10,"for fun")); //不能转给自己
            cb2.add(new Transaction(accounts.get("Tover"),accounts.get("KunLin"),5,"haha"));
            cb2.add(new Transaction(accounts.get("KunLin"),accounts.get("Tover"),1,"hehe"));
            cb2.add(new Transaction(accounts.get("Tover"),accounts.get("KunLin"),0.5,"mmp"));
            createTransaction(cb2,accounts.get("KunLin"));

            System.out.println("Balance for SYSTEM: \t" + accounts.get("__@SYSTEM__").getBalance("admin"));
            //accounts.get("Tover").refreshBalance(blockChain);  //刷新余额，用于校验
            System.out.println("Balance for Tover: \t" + accounts.get("Tover").getBalance("abc"));
            System.out.println("Balance for KunLin: \t" + accounts.get("KunLin").getBalance("aaa"));

            System.out.println("Print chain:\n" + blockChain.toString());
        }
        catch(Exception e){
            System.out.println(e);
        }


    }

}
