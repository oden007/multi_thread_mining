package app;

//ブロックチェーンにおけるマイニング処理を並列化させることによるマイニングにかかる時間の短縮を目的とプログラム
import java.security.MessageDigest;
import java.util.Date;
import java.util.Random;
import java.util.ArrayList;

//ブロックチェーンクラス
class Blockchain{
    Long nonce;//ナンス(マイニングでは本項目を探索する)
    Date timestamp;//ブロックを作成した時間
    String previous_hash;//自身の前のブロックのハッシュ値
    int cash_data;//トランザクションデータ
    String tarnsaction;//トランザクションデータ
    String hash;//自身のハッシュ値
    int difficulty=6;//先頭からの何ビットが0であるのかを示す(マイニングの成功率は1/16^difficulty))
    //difficultyがマイニングの難易度に直結するのでマイニング時間に応じて変更させる(デフォルト：6)
    public Blockchain(String previous_hash,int cash_data,String transaction){
        this.timestamp=new Date();
        this.cash_data=cash_data;
        this.tarnsaction=transaction;
        this.previous_hash=previous_hash;

    }
    //中身を表示する為のメソッド
    public void show_block(){
        System.out.println("作成したブッロクの内容\nprevious hash : "+this.previous_hash+"\ncash data : "+
        this.cash_data+"\ntransaction data :"+this.tarnsaction+"\ntime stamp :"+this.timestamp);
    }
}

//SHA-256(ハッシュ化)を行うクラス
class Sha256{
    public static String sha_256(String plain){
    byte[] cipher_byte;
    try{
        MessageDigest md = MessageDigest.getInstance("SHA-256");
//md.update(text.getBytes());
md.update(plain.getBytes());
cipher_byte = md.digest();
StringBuffer sb = new StringBuffer(2 * cipher_byte.length);
 for(byte b: cipher_byte) {
    sb.append(String.format("%02x", b&0xff) );
    }
    return sb.toString();
    }
   catch (Exception e) {
        e.printStackTrace();
} 
return plain;
}
    }
 
//作成したブロックが正しいかどうか確認を行うクラス
class Check_block{
    Blockchain previous_block;
    Blockchain current_block;
public Check_block(Blockchain pre_block,Blockchain block){
this.previous_block=pre_block;
this.current_block=block;
}

public Boolean confirm_block() {
if(!previous_block.hash.equals(current_block.previous_hash))
return false;
if(!current_block.hash.equals(Sha256.sha_256(current_block.previous_hash+current_block.cash_data
+current_block.tarnsaction+current_block.timestamp+current_block.nonce)))
return false;
return true;
}

}
//マイニングを行うクラス
class Mining{
    Random random = new Random();
    long mining;
    String search;
    String tmp_hash;        
    Boolean loop=false;
    long q=0L;
    public long Search_nonce(Blockchain block,long nonce,int number,int thread_number){
    //マイニング開始前に使用する変数の初期化をする
    loop=false;
    q=0L;
    mining=nonce;
    String target = new String(new char[block.difficulty]).replace('\0', '0');
    //System.out.println("マイニング開始");
    while(!loop){
        try{
    //先頭から0が何bit連続しているのか測定
    tmp_hash=Sha256.sha_256(block.previous_hash+block.cash_data+block.tarnsaction+block.timestamp+mining);
    q++;
    if(tmp_hash.substring(0,block.difficulty).equals(target)){
            block.nonce=mining;
            block.hash=tmp_hash.toString();
            loop=true;
            System.out.println("Thread number : "+thread_number+"-探索回数->("+q+")回");
            return q;
    }
    mining+=number;
    }
catch (Exception e) {
    e.printStackTrace();
}
    }
return q;
}
public long stop_mining(boolean loop){
this.loop=loop;
return q;
}
}
//スレッド処理を行う為に作成するクラス
class MultiThread extends Thread {
    long nonce;
    int number;
    boolean thread_going=false;
    Mining mining_nonce= new Mining();
    Blockchain block;
    int thread_number;
    long search_number;
    public MultiThread(long nonce,int thread_number,int number,Blockchain block){
        this.nonce=nonce;
        this.thread_number=thread_number;
        this.number=number;
        this.block=block;
        search_number=0;
    }
    public void run() {
        search_number=mining_nonce.Search_nonce(block,nonce,number,thread_number);
    }
    public void stopRunning(){
        search_number=mining_nonce.stop_mining(true);
      }
}

class Block_chain{
   public static ArrayList<Blockchain> block = new ArrayList<Blockchain>();
    public static void main(String[] args){
        Boolean confirm=false;
        long mining=0;//マイニングに使用するための変数
        ArrayList <MultiThread> list=new ArrayList<>();
        ArrayList <Boolean> confirm_thread=new ArrayList<>();
        int Thread_number=4;//作成するスレッドの数、多いほどマイニングの高速化が見込める(使用する環境のCPUに応じて変更、デフォルト：4)
 
        for(int i=0;i<3;i++){
            //ジェネシスブロック作成
            Random random = new Random();
        //取引に使用するデータ
        int cash=random.nextInt(20000)+1;
        String transaction;
        //偶数・奇数で処理を分岐
        if(cash%2==0){
            transaction="increase";
        }else{
            transaction="decrease";
        }
            if(block.size()==0)
        block.add(new Blockchain("Genesis_hash",cash,transaction));
            else
        block.add(new Blockchain(block.get(block.size()-1).hash, cash,transaction));
        long startTime = System.currentTimeMillis();
        mining= (random.nextInt(20)+1)*1000000000L;
        //スレッドの追加(リストに追加次第、各スレッドのマイニング開始)
        for(int j=0;j<Thread_number;j++){
        list.add(new MultiThread(mining+j,j+1,Thread_number,block.get(block.size()-1)));
        confirm_thread.add(true);
        list.get(j).start();
        }
        System.out.println("マイニング作業"+(i+1)+"回目");
        //スレッドの作業待ち
        int thread_stop=0;
 //作成したthreadの管理
    while(true){
    for(int k=0,j=list.size();k<j;k++){
    if(!list.get(k).isAlive()){
    confirm_thread.set(k,false);
    thread_stop=k;
    break;
}
}
//threadを格納しているリストの中にisAlive()がfalseとなっているものを探索する
if(confirm_thread.contains(false)){
    //他のスレッドを終了させる
for(int k=0,j=list.size();k<j;k++){
    if(k!=thread_stop){
        list.get(k).stopRunning();
    }
}
    break;
}
};//ここまでが親スレッドによる管理
        long endTime = System.currentTimeMillis();
        //探索の総計
        long search_total=0;
        for(int k=0,j=list.size();k<j;k++){
                search_total+=list.get(k).search_number;
            }
        System.out.println("探索回数の合計値->("+search_total+")回");
        //マイニングにかかった時間や算出したハッシュ値、nonceの表示
        long min=(endTime-startTime)/60000;
        long sec=((endTime-startTime)-60000*min)/1000;
        System.out.println("処理時間：" + min +" min "+ sec+" sec "+ ((endTime - startTime)-60000*min-1000*sec) + " ms");
        System.out.println("nonce->"+block.get(i).nonce);
        System.out.println("SHA-256->"+block.get(i).hash);
        //不正なブロックであるかどうかの確認
        if(block.size()>1){
            Check_block check = new Check_block(block.get(block.size()-2),block.get(block.size()-1));
            confirm=check.confirm_block(); 
        if(confirm)
        System.out.println("正しいブロックが作成されました");
            else
        System.out.println("間違ったブロックが生成されました");
    }
        //Thread,Thread監視用リストの初期化
        list.clear();
        confirm_thread.clear();
        //作成したブロックの表示
        block.get(i).show_block();
        }
}
}

