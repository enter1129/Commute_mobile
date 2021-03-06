package samsungkh.com.commute_moblie;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TimePicker;

import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.honorato.multistatetogglebutton.ToggleButton;

import java.util.ArrayList;

import static samsungkh.com.commute_moblie.R.array.planets_array;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    //하위호환성 문제로 support LIB 를 import해야 함.
    SearchView searchView;
    MultiStateToggleButton gubunButton;

    ArrayList<RouteVO> datas;
    ListView listView;
    Button main_time;
    int hour, minute;
    String time, gubunStr;

    Resources res;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set time button
        initTimeButton();
        //set gubun multiButton
        initGubunButton();

        listView = (ListView) findViewById(R.id.main_list);
        listView.setOnItemClickListener(this);
    }

    //set time button
    private void initTimeButton(){
        Button button = (Button)this.findViewById(R.id.main_time);
        main_time = button;

        //최초 로딩 시 출근 기준 시간 세팅
        main_time.setText("06:00");

//        Calendar calendar = Calendar.getInstance();
//        hour = calendar.get(Calendar.HOUR_OF_DAY);
//        minute = calendar.get(Calendar.MINUTE);
//        main_time.setText(String.format("%02d",hour)+":"+String.format("%02d",minute));

        //시간 클릭 이벤트
        findViewById(R.id.main_time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String time = main_time.getText().toString();
                if(!(time).equals("") || time != null){
                    new TimePickerDialog(MainActivity.this, timeSetListener, Integer.parseInt(time.substring(0,2)) , Integer.parseInt(time.substring(3)), false).show();
                }else{
                    new TimePickerDialog(MainActivity.this, timeSetListener, hour, minute, false).show();
                }
            }
        });
    }

    //출퇴근 버튼 클릭 이벤트
    private void initGubunButton(){

        gubunButton = (MultiStateToggleButton)findViewById(R.id.main_gubun_multi_toggle);
        gubunButton.setValue(0);
        gubunButton.setOnValueChangedListener(new ToggleButton.OnValueChangedListener() {
            @Override
            public void onValueChanged(int position) {

                time = main_time.getText().toString();
                String[] gubunArray = res.getStringArray(planets_array);
                gubunStr = gubunArray[gubunButton.getValue()];

                searchFun(null, time, gubunStr);
            }
        });
    }

    //TimePicker
    private TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            String msg = String.format("%02d:%02d", hourOfDay, minute);
            main_time.setText(msg);

            time = main_time.getText().toString();
            String[] gubunArray = res.getStringArray(planets_array);
            gubunStr = gubunArray[gubunButton.getValue()];
            searchFun(null, time, gubunStr);
        }
    };

    @Override
    protected void onResume() {
        //라이프사이클 상위클래스 상속은 지우면 안됨!!!!
        super.onResume();

        time = main_time.getText().toString();
        res = getResources();
        String[] gubunArray = res.getStringArray(planets_array);
        gubunStr = gubunArray[gubunButton.getValue()];

        //최초 로딩 시 전체 리스트 뿌려주기(단 최초 세팅된 시간 및 구분(출퇴근)값은 넘김)
        searchFun(null, time, gubunStr);
    }

    //메뉴구성을 위해 자동으로 콜되는 함수
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //메뉴 객체 획득하고 그 객체에 포함된 SearchView 획득
        MenuItem menuItem = menu.findItem(R.id.menu_main_search);
        searchView=(SearchView) MenuItemCompat.getActionView(menuItem);  //dprecated되었지만 하위호환성문제때문에 사용(최근에 됨)
        //hint문자열
        searchView.setQueryHint(getResources().getString(R.string.main_search_hint));
        //초기아이콘으로셋팅
        searchView.setIconifiedByDefault(true);

        //이벤트
        searchView.setOnQueryTextListener(queryListener);  //하단이벤트 참조

        return super.onCreateOptionsMenu(menu);
    }

    //Inline query
    SearchView.OnQueryTextListener queryListener = new SearchView.OnQueryTextListener() {

        //검색버튼 눌렀을 때 call
        @Override
        public boolean onQueryTextSubmit(String query) {
            //검색어 초기화
            searchView.setQuery("", false);
            searchView.setIconified(true);

            //로직처리부분
            time = main_time.getText().toString();
            String[] gubunArray = res.getStringArray(planets_array);
            gubunStr = gubunArray[gubunButton.getValue()];

            //구분으로 보내기 위한 조회조건 처리
            searchFun(query, time, gubunStr);

            return false;
        }

        //검색어 입력 시 마다 call(추천단어에 사용)
        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(this, ReadStopActivity.class);

        //detail 조회를 위한 id값 intent로 넘기기
        intent.putExtra("rt_id", datas.get(i).rt_id);
        intent.putExtra("rt_nm", datas.get(i).rt_nm);
        intent.putExtra("time", time);
        intent.putExtra("gubun", gubunStr);
        startActivity(intent);
    }

    public void searchFun(String searchStr, String time, String gubun){

        String timeStr = (time).replace(":","");

        //메인화면 리스트 뿌려질 데이터 가져오기
        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = helper.openDatabase();
        Cursor cursor;
        if(searchStr == null || searchStr.equals("") ){
            cursor = db.rawQuery(
                    "SELECT DISTINCT A.ROUTE_ID, A.ROUTE_DESC, Z.GUGAN "+
                    "FROM CM_ROUTE A, CM_TIMETABLE B "+
                    ",(SELECT A.ROUTE_ID, D.STOP_DESC ||' ~ '||C.STOP_DESC AS GUGAN FROM "+
                    " (SELECT A.ROUTE_ID ,MAX(ROUTE_SEQ) MAX_STOP	FROM CM_ROUTE A GROUP BY A.ROUTE_ID ) A, "+
                    " (SELECT A.ROUTE_ID ,MIN(ROUTE_SEQ) MIN_STOP FROM CM_ROUTE A GROUP BY A.ROUTE_ID ) B, "+
                    " CM_ROUTE C, CM_ROUTE D "+
                    " WHERE 1=1 AND A.ROUTE_ID = C.ROUTE_ID AND B.ROUTE_ID = C.ROUTE_ID AND C.ROUTE_ID = D.ROUTE_ID AND A.MAX_STOP = C.ROUTE_SEQ AND B.MIN_STOP = D.ROUTE_SEQ) Z "+
            "WHERE A.ROUTE_ID = B.ROUTE_ID "+
                    "AND A.ROUTE_ID = Z.ROUTE_ID "+
                    "AND B.DIRECTION = ?  "+
                    "AND cast(replace(B.DEPART_TIME,':','') as integer) > cast(? as integer) "+
                    "ORDER BY A.ROUTE_DESC ASC ", new String[]{gubun, timeStr});
        }else{
            cursor = db.rawQuery(
                    "SELECT DISTINCT A.ROUTE_ID, A.ROUTE_DESC, Z.GUGAN  "+
                            "FROM CM_ROUTE A, CM_TIMETABLE B "+
                            ",(SELECT A.ROUTE_ID, D.STOP_DESC ||' ~ '||C.STOP_DESC AS GUGAN FROM "+
                            " (SELECT A.ROUTE_ID ,MAX(ROUTE_SEQ) MAX_STOP	FROM CM_ROUTE A GROUP BY A.ROUTE_ID ) A, "+
                            " (SELECT A.ROUTE_ID ,MIN(ROUTE_SEQ) MIN_STOP FROM CM_ROUTE A GROUP BY A.ROUTE_ID ) B, "+
                            " CM_ROUTE C, CM_ROUTE D "+
                            " WHERE 1=1 AND A.ROUTE_ID = C.ROUTE_ID AND B.ROUTE_ID = C.ROUTE_ID AND C.ROUTE_ID = D.ROUTE_ID AND A.MAX_STOP = C.ROUTE_SEQ AND B.MIN_STOP = D.ROUTE_SEQ) Z "+
                            "WHERE A.ROUTE_ID = B.ROUTE_ID "+
                            "AND A.ROUTE_ID = Z.ROUTE_ID "+
                            "AND (A.ROUTE_DESC LIKE ?  OR A.STOP_DESC LIKE ? ) "+
                            "AND B.DIRECTION = ?  "+
                            "AND cast(replace(B.DEPART_TIME,':','') as integer) > cast(? as integer) "+
                            "ORDER BY A.ROUTE_DESC ASC "
            , new String[]{"%"+searchStr+"%", "%"+searchStr+"%", gubun, timeStr});
        }

        //추출데이터 SET
        datas = new ArrayList<RouteVO>();
        while(cursor.moveToNext()){
            RouteVO vo = new RouteVO();
            vo.rt_id = cursor.getString(0);
            vo.rt_nm = cursor.getString(1);
            vo.gugan = cursor.getString(2);
            datas.add(vo);
        }

        db.close();

        //Adapter 처리, 즉, 커스텀 리스트뷰를 만들어서 main activity의 main_list_item로 세팅해주는 부분
        MainListAdapter adapter = new MainListAdapter(this, R.layout.main_list_item, datas);
        listView.setAdapter(adapter);

    }
}
