package com.dsm.eatting;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    Calendar calendar = Calendar.getInstance();         //날짜 관련 작업을 하는 객체
    int week = calendar.get(Calendar.DAY_OF_WEEK);   //일주일 중 현재 요일을 구하는데 사용되는 변수
    String nowday=doDayOfWeek(week);                    //현재 요일 정보를 담는 문자열
    String strDate = (calendar.get(Calendar.MONTH)+1) + "/" + calendar.get(Calendar.DAY_OF_MONTH) + "(" + nowday + ")";   //날짜 란에 표시될 문자열
    String nowMeal;                                     //표시되는 식단이 나오는 시간대를 저장하는 문자열
    int daymod=0;                                       //현재 시간대를 설정하는데 쓰이는 변수
    int todayNum=calendar.get(Calendar.DAY_OF_WEEK)-2;                 //일주일 중 오늘을 찾는 데 쓰이는 변수

    public TextView tdate;                              //날짜가 표시될 뷰 객체
    public TextView tmenu;                              //식단이 표시될 뷰 객체
    String mealSearch;                                  //검색하고 싶은 음식을 저장해놓는 문자열
    EditText msearch;                                   //검색하고 싶은 음식을 입력받는 검색창 객체
    Button bsearch;                                     //누르면 음식 검색을 동작하게하는 버튼 객체
    Button bprev;                                       //누르면 이전 식단을 보여주는 동작을 하게하는 버튼 객체
    Button bnext;                                       //누르면 다음 식단을 보여주는 동작을 하게하는 버튼 객체

    int URLYear=calendar.get(Calendar.YEAR);
    int URLMonth=calendar.get(Calendar.MONTH)+1;
    int URLDay=calendar.get(Calendar.DAY_OF_MONTH);
    String mealURL="http://www.dsm.hs.kr/segio/meal/meal.php?year="+URLYear+"&month="+URLMonth+"&day="+URLDay;      //파싱해올 홈페이지 주소를 저장하는 문자열
    String[][] mealTable=new String[3][7];             //일주일치 식단이 저장되는 문자열 배열

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tdate = (TextView) findViewById(R.id.Tdate);                //객체를 xml레이아웃 부분과 연결
        tmenu = (TextView) findViewById(R.id.Tmenu);
        msearch=(EditText)findViewById(R.id.Msearch);
        bsearch=(Button)findViewById(R.id.Bsearch);
        bprev=(Button)findViewById(R.id.Bprev);
        bnext=(Button)findViewById(R.id.Bnext);

        ConnectivityManager manager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);        //네트워크 권한 획득용 객체
        NetworkInfo mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);       //데이터 네트워크에 연결되어있는지 확인용 객체
        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);           //WIFI에 연결되어있는지 확인용 객체

        if (wifi.isConnected() || mobile.isConnected()) {                   //네트워크에 연결되있다면 그냥 다음으로 넘어감
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "인터넷에 연결되어있지 않습니다. 마지막에 저장된 정보를 불러옵니다.", Toast.LENGTH_LONG);
            toast.show();               //토스트 메시지 출력
            //moveTaskToBack(true);
            //finish();
            try {
                //FileInputStream fis;            //파일 불러오기용 객체
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File f;
                BufferedReader buffer;          //파일 읽기용 객체
                for(int i=0;i<7;i++) {
                    for (int j = 0; j < 3; j++) {
                        String fileName="mealTable"+Integer.toString(i)+Integer.toString(j)+".txt";     //파일 이름 설정
                        f = new File(path, fileName);
                        //fis = openFileInput(fileName);                                                     //파일 불러오기
                        //buffer = new BufferedReader(new InputStreamReader(fis));                          //읽어올 파일 설정
                        buffer = new BufferedReader(new FileReader(f));
                        mealTable[i][j]=buffer.readLine();                                               //데이터 추출뒤 배열에 저장
                        buffer.close();                                                                    //버퍼 종료
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ParseStart();      //파싱 시작
        if (calendar.get(Calendar.HOUR_OF_DAY) < 8) {           //오전 8시 이전은 아침 식단 표시
            nowMeal = "아침";
            daymod=1;
        } else if (calendar.get(Calendar.HOUR_OF_DAY) < 13) {  //오후 1시 전까지는 점심 식단 출력
            nowMeal = "점심";
            daymod=2;
        } else {                                                  //다음날 전까지는 계속 저녁 출력
            nowMeal = "저녁";
            daymod=3;
        }
        displaySet();                                             //화면 설정 메소드로 넘김

        bsearch.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){                 //검색 버튼 기능
                mealSearch=msearch.getText().toString();       //입력한 음식 문자열에 저장
                searchResult();                                   //검색 메소드로 넘김
            }
        });

        bprev.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {                   //이전 버튼 기능
                if (daymod == 1) {                              //아침 식단 표기중이면 어제 저녁 식단으로 넘어감
                    todayNum -= 1;
                    daymod = 3;
                    calendar.add(calendar.DAY_OF_MONTH, -1); //객체 날짜를 어제로 설정
                    if(week==1){                                 //일주일 시작점에 오면 전주 끝으로 설정
                        week=7;
                        URLYear=calendar.get(Calendar.YEAR);
                        URLMonth=calendar.get(Calendar.MONTH)+1;
                        URLDay=calendar.get(Calendar.DAY_OF_MONTH);
                        mealURL="http://www.dsm.hs.kr/segio/meal/meal.php?year="+URLYear+"&month="+URLMonth+"&day="+URLDay;
                        ParseStart();
                    }else{                                       //일주일 중 현재 위치에서 어제로 이동
                        week-=1;
                    }
                } else {                                         //이전 식단으로 설정
                    daymod -= 1;
                }
                displaySet();                                     //화면 재설정
            }
        });

        bnext.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {                   //다음 버튼 기능
                if (daymod == 3) {                              //저녁 식단 표기중이면 내일 아침 식단으로 넘어감
                    todayNum += 1;
                    daymod = 1;
                    calendar.add(calendar.DAY_OF_MONTH, 1);  //객체 날짜를 하루 내일로 설정
                    if(week==7){                                 //일주일 끝점에 오면 다음주 시작점으로 설정
                        week=1;
                        URLYear=calendar.get(Calendar.YEAR);
                        URLMonth=calendar.get(Calendar.MONTH)+1;
                        URLDay=calendar.get(Calendar.DAY_OF_MONTH);
                        mealURL="http://www.dsm.hs.kr/segio/meal/meal.php?year="+URLYear+"&month="+URLMonth+"&day="+URLDay;
                        ParseStart();
                    }else{                                       //일주일 중 현재 위치에서 내일로 이동
                        week+=1;
                    }
                } else {                                         //다음 식단으로 설정
                    daymod += 1;
                }
                displaySet();                                     //화면 재설정
            }
        });
    }

    private void ParseStart(){
        JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
        jsoupAsyncTask.execute();
    }

    private void searchResult(){                                //입력한 음식을 검색하는 메소드
        boolean ifFind=false;                                   //음식을 찾았는지 확인용 변수
        Toast toast;                                              //검색 결과를 표시할 객체
        String SR="";                                             //검색 결과를 표시하는 데 사용되는 문자열
        for(int i=0;i<3;i++){
            for(int j=0;j<7;j++){
                if(mealTable[i][j].contains(mealSearch)){      //식단 배열안에 찾는 음식이 있으면
                    SR="해당 음식은 이번주 ";                   //"해당 음신은 이번주 X요일(XX)에 있습니다."라고 문자열에 저장
                    switch (j){                                  //나오는 요일 찾기
                        case 0:
                            SR+="월요일";
                            break;
                        case 1:
                            SR+="화요일";
                            break;
                        case 2:
                            SR+="수요일";
                            break;
                        case 3:
                            SR+="목요일";
                            break;
                        case 4:
                            SR+="금요일";
                            break;
                        case 5:
                            SR+="토요일";
                            break;
                        case 6:
                            SR+="일요일";
                            break;
                        default:
                            SR="ERROR";
                            break;
                    }
                    switch (i){                                  //나오는 시간대 찾기
                        case 0:
                            SR+="(아침)";
                            break;
                        case 1:
                            SR+="(점심)";
                            break;
                        case 2:
                            SR+="(저녁)";
                            break;
                        default:
                            SR="ERROR";
                            break;
                    }
                    SR+="에 있습니다.";
                    ifFind=true;                                 //찾았음을 알림
                }
            }
        }
        if(ifFind==false) {                                     //찾지 못했다면 못찾음을 알림
            SR="해당 음식은 이번주에 나오지 않습니다.";
        }
        toast = Toast.makeText(getApplicationContext(), SR, Toast.LENGTH_LONG);     //토스트 메시지 설정
        toast.show();                                                                 //토스트 메시지 출력
    }

    private void displaySet(){                                                      //보이는 화면에 뜨는 텍스트를 설정하는 메소드
        nowday=doDayOfWeek(week);                                                   //요일 찾기
        strDate = (calendar.get(Calendar.MONTH)+1) + "/" + calendar.get(Calendar.DAY_OF_MONTH) + "(" + nowday + ")";    //날짜를 문자열에 저장
        switch (daymod){                                                                                                       //시간대에 따른 식단 출력
            case 1:
                nowMeal="아침";
                tmenu.setTextColor(Color.parseColor("#228B22"));
                tmenu.setText(mealTable[0][todayNum]);
                break;
            case 2:
                nowMeal="점심";
                tmenu.setTextColor(Color.parseColor("#0000CD"));
                tmenu.setText(mealTable[1][todayNum]);
                break;
            case 3:
                nowMeal="저녁";
                tmenu.setTextColor(Color.parseColor("#9400D3"));
                tmenu.setText(mealTable[2][todayNum]);
                break;
            default:
                nowMeal="Error";
                tmenu.setText("식단을 표시할 수 없습니다.");
                break;
        }
        tdate.setText(strDate + nowMeal);                                         //날짜 출력
    }

    private class JsoupAsyncTask extends AsyncTask<Void, Void, Void> {            //홈페이지를 파싱하는 메소드
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Document doc = Jsoup.connect(mealURL).get();                        //파싱해올 홈페이지 설정
                Element breakfast;                                                   //아침시간대 식단을 가져오는 객체
                Element lunch;                                                       //점심시간대 식단을 가져오는 객체
                Element dinner;                                                      //저녁시간대 식단을 가져오는 객체
                for(int i=0;i<7;i++){
                    breakfast = doc.select("table.meal_table:nth-child(3) > tbody:nth-child(3) > tr:nth-child(2) > td").get(i);       //각각 시간대에 맞춰서 식단 파싱
                    lunch = doc.select("table.meal_table:nth-child(6) > tbody:nth-child(3) > tr:nth-child(2) > td").get(i);
                    dinner = doc.select("table.meal_table:nth-child(9) > tbody:nth-child(3) > tr:nth-child(2) > td").get(i);
                    mealTable[0][i]=breakfast.text();                               //파싱한 식단을 시간대에 맞춰서 배열에 저장
                    mealTable[1][i]=lunch.text();
                    mealTable[2][i]=dinner.text();
                    //#sub_context > div > div.meal > table > tbody > tr:nth-child(14) > td.today
                    //#sub_context > div > div.meal > table > tbody > tr:nth-child(15) > td.today
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File f;
                FileWriter write;
                //FileOutputStream fos;                                                //파일을 생성하는 객체
                PrintWriter out;                                                     //파일에 입력하는 객체
                for(int i=0;i<7;i++){
                    for(int j=0;j<3;j++) {
                        String fileName="mealTable"+Integer.toString(i)+Integer.toString(j)+".txt";         //파일 이름 설정
                        //fos = openFileOutput(fileName, Context.MODE_APPEND);                                 //텍스트 파일 생성
                        //out = new PrintWriter(fos);                                                           //입력할 파일 설정
                        f = new File(path, fileName);
                        write = new FileWriter(f, false);
                        out = new PrintWriter(write);
                        out.println(mealTable[i][j]);                                                        //파일에 식단 입력
                        out.close();                                                                           //입력 객체 종료
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            displaySet();
        }              //화면 출력 설정
    }

    private String doDayOfWeek(int nWeek) {                                       //요일을 구하는 메소드
        String strWeek;
        switch (nWeek) {                                                           //요일 정보를 문자로 반환
            case 1:
                strWeek = "일";
                tdate.setTextColor(Color.parseColor("#F11E22"));
                break;
            case 2:
                strWeek = "월";
                break;
            case 3:
                strWeek = "화";
                break;
            case 4:
                strWeek = "수";
                break;
            case 5:
                strWeek = "목";
                break;
            case 6:
                strWeek = "금";
                break;
            case 7:
                strWeek = "토";
                tdate.setTextColor(Color.parseColor("#0000FF"));
                break;
            default:
                strWeek = "Error";
                break;
        }
        return strWeek;
    }
}
