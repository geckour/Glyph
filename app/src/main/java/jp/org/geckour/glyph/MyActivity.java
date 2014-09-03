package jp.org.geckour.glyph;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class MyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyView view = new MyView(this);
        setContentView(view);

        ActionBar actionBar = getActionBar();
        actionBar.hide();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class MyView extends View {
        private final Handler handler = new Handler();
        boolean state = true;
        boolean doCount = true;
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        float offsetX, offsetY;
        Paint p = new Paint();
        double cr = Math.PI/3;
        double radius;
        PointF[] dots = new PointF[11];
        ArrayList<Point> Locus = new ArrayList<Point>();
        Path locusPath = new Path();
        int framec = 0;
        boolean[] isThrough = new boolean[11];
        ThroughList[] throughList;
        ThroughList[] answerThroughList;
        int qTotal = 0;
        int qNum = 0;
        int defTime = 200;
        HashMap<String, ThroughList> shapes = new HashMap<String, ThroughList>();
        ArrayList<ShapesSet> shapesSets = new ArrayList<ShapesSet>();
        boolean isFirstOnTimeup = true;
        ArrayList<Difficulty> difficulty = new ArrayList<Difficulty>();
        boolean isStartGame = false;

        public class ThroughList {
            ArrayList<Integer> dots;
            public ThroughList(){
                dots = new ArrayList<Integer>();
            }
            public ThroughList(ArrayList<Integer> argDots){
                dots = new ArrayList<Integer>(argDots);
            }
        }

        public class ShapesSet {
            ArrayList<String> strings;
            ArrayList<String> correctString;
            public ShapesSet(ArrayList<String> argStrings) {
                strings = argStrings;
            }
            public ShapesSet(ArrayList<String> argStrings, ArrayList<String> argCorrectString) {
                strings = argStrings;
                correctString = argCorrectString;
            }
        }

        public class Difficulty {
            int qs = 0;
            int time = 0;
            public Difficulty(int argQs, int argTime) {
                qs = argQs;
                time = argTime;
            }
        }

        public MyView(Context context) {
            super(context);

            ArrayList<Integer> giveDot;
            int counter = 0;
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 2, 9, 8));
            shapes.put("ABANDON", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 1));
            shapes.put("ADAPT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3, 9));
            shapes.put("ADVANCE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 0, 4, 1));
            shapes.put("AGAIN", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 7, 8, 9, 10, 5));
            shapes.put("ALL", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1, 0));
            shapes.put("ANSWER", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 5, 4, 7));
            shapes.put("ATTACK", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 5, 4, 6, 1));
            shapes.put("AVOID", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 1, 7));
            shapes.put("BARRIER", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 2, 8, 1));
            shapes.put("BEGIN", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 3, 4, 1, 8));
            shapes.put("BEING", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 3, 4));
            shapes.put("BODY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 4, 6));
            shapes.put("BREATHE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 1, 0, 2, 9, 8));
            shapes.put("CAPTURE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 8, 1));
            shapes.put("CHANGE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 10, 5, 6, 4, 0, 2, 8));
            shapes.put("CHAOS", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 8));
            shapes.put("CLEAR", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 8, 9, 10, 5, 6, 7, 8));
            shapes.put("CLEAR ALL", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 0, 2));
            shapes.put("COMPLEX", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 1, 4, 7));
            shapes.put("CONFLICT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 1, 7));
            shapes.put("CONSEQUENCE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 7, 8, 2, 3, 0, 4));
            shapes.put("CONTEMPLATE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 4, 7));
            shapes.put("CONTRACT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 1));
            shapes.put("COURAGE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 6));
            shapes.put("CREATE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 8, 2, 10, 3, 0));
            shapes.put("CREATIVITY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 9, 10, 3, 0, 1, 7, 6, 4));
            shapes.put("MIND", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3, 0, 8));
            shapes.put("DANGER", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 8, 1, 6));
            shapes.put("DEFEND", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 7, 8));
            shapes.put("DESTINATION", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 4, 1, 2, 8));
            shapes.put("DESTINY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 1, 7));
            shapes.put("DESTROY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 1, 7));
            shapes.put("DIE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1, 4, 6));
            shapes.put("DIFFICULT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 7, 8, 9));
            shapes.put("DISCOVER", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 10, 9));
            shapes.put("DISTANCE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 0, 2, 8));
            shapes.put("EASY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 5, 6, 1, 8));
            shapes.put("END", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 7, 6, 5, 3, 0, 4, 3));
            shapes.put("ENLIGHTENED_A", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 8, 7, 6, 5, 3, 0, 4, 3));
            shapes.put("ENLIGHTENED_B", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1));
            shapes.put("EQUAL", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 4, 3, 2));
            shapes.put("ESCAPE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 3, 2));
            shapes.put("EVOLUTION", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 4, 1));
            shapes.put("FAILURE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1, 6));
            shapes.put("FEAR", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 6, 7));
            shapes.put("FOLLOW", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 9));
            shapes.put("FORGET", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 1, 7));
            shapes.put("FUTURE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2));
            shapes.put("GAIN", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 1, 4, 6));
            shapes.put("GOVERNMENT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2));
            shapes.put("GROW", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(7, 1, 0, 3, 5, 4, 0));
            shapes.put("HARM", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 1, 8, 2, 0, 4, 5, 3, 0));
            shapes.put("HARMONY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 0, 2, 8));
            shapes.put("HAVE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 2, 1));
            shapes.put("HELP", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 5, 6, 1, 8));
            shapes.put("END", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 6, 1, 2));
            shapes.put("HIDE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 4, 8));
            shapes.put("I", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 7));
            shapes.put("IGNORE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 0, 2, 3, 0));
            shapes.put("IMPERFECT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 1));
            shapes.put("IMPROVE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 3, 2, 0));
            shapes.put("IMPURE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 3, 10, 9, 2, 0, 8));
            shapes.put("INTERRUPT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 3, 10, 9, 8));
            shapes.put("JOURNEY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 0, 4, 8));
            shapes.put("KNOWLEDGE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 10, 9, 2, 8));
            shapes.put("LEAD", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 10, 5, 6, 4, 1, 7));
            shapes.put("LEGACY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 4));
            shapes.put("LESS", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 0, 4, 6, 5));
            shapes.put("LIBERATE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 0, 1, 4, 0));
            shapes.put("LIE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 0, 4, 6));
            shapes.put("LIVE AGAIN", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 1));
            shapes.put("LOSE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 0, 1, 6));
            shapes.put("MESSAGE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 3, 0, 8));
            shapes.put("IDEA", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1));
            shapes.put("MORE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 5, 4, 3, 2));
            shapes.put("MYSTERY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 4, 1, 7));
            shapes.put("NATURE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 1, 7));
            shapes.put("NEW", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1));
            shapes.put("NO", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 9, 2, 0, 8));
            shapes.put("NOURISH", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2));
            shapes.put("OLD", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 1, 8));
            shapes.put("OPEN", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 1, 8, 9, 10, 5, 6, 7, 8));
            shapes.put("OPEN ALL", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 4, 6, 7, 1, 2, 9, 10));
            shapes.put("OPENING", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 9));
            shapes.put("PAST", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 2, 9));
            shapes.put("PATH", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 2, 9, 8, 7, 1, 0));
            shapes.put("PERFECTION", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 5, 3, 0, 1, 7));
            shapes.put("PERSPECTIVE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 1, 7, 6));
            shapes.put("POTENTIAL", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 0, 4, 1, 2, 8, 1));
            shapes.put("PRESENCE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 2, 1, 4));
            shapes.put("PRESENT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 4, 1, 0));
            shapes.put("PURE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 5, 4));
            shapes.put("PURSUE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 3, 2, 9));
            shapes.put("CHASE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 3, 2));
            shapes.put("QUESTION", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 0, 1, 7));
            shapes.put("REACT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 4, 6, 7));
            shapes.put("REBEL", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 3, 10, 5, 0));
            shapes.put("RECHARGE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 5, 0, 8, 2));
            shapes.put("RESISTANCE_A", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 5, 0, 8, 1));
            shapes.put("RESISTANCE_B", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 1, 7, 8));
            shapes.put("RESTRAINT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 7));
            shapes.put("RETREAT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 4, 7));
            shapes.put("SAFETY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1, 6));
            shapes.put("SAVE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3));
            shapes.put("SEE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 4, 3, 2, 1));
            shapes.put("SEEK", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 8, 7));
            shapes.put("SELF", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 0, 4, 1, 7));
            shapes.put("SEPARATE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 5, 4, 1, 7));
            shapes.put("SHAPER", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(7, 1, 2, 9, 8));
            shapes.put("SHARE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 1));
            shapes.put("SIMPLE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 4, 1, 8));
            shapes.put("SOUL", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 1, 7));
            shapes.put("STABILITY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1, 2));
            shapes.put("STRONG", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 3, 0));
            shapes.put("TOGETHER", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 1, 4, 0, 2, 3));
            shapes.put("TRUTH", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 1, 6));
            shapes.put("USE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 5, 4, 8));
            shapes.put("VICTORY", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 8, 1));
            shapes.put("WANT", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 8));
            shapes.put("WE", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 4, 1));
            shapes.put("WEAK", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 1, 6));
            shapes.put("WORTH", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1, 0, 2));
            shapes.put("XM", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 2, 1, 5));
            shapes.put("YOU", new ThroughList(giveDot));
            shapes.put("" + counter, new ThroughList(giveDot));

            ArrayList<String> giveStrings;
            ArrayList<String> collectStrings;
            giveStrings = new ArrayList<String>(Arrays.asList("BEING", "PAST", "PRESENT", "FUTURE"));
            collectStrings = new ArrayList<String>(Arrays.asList("HUMAN", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, collectStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "PURE", "FUTURE", "BEING", "GOVERNMENT"));
            collectStrings = new ArrayList<String>(Arrays.asList("", "", "", "HUMAN", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, collectStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISTANCE", "SELF", "AVOID", "BEING", "LIE"));
            collectStrings = new ArrayList<String>(Arrays.asList("OUTSIDE", "", "", "HUMAN", "LIE"));
            shapesSets.add(new ShapesSet(giveStrings, collectStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ADVANCE", "GOVERNMENT", "AGAIN", "FAILURE"));
            collectStrings = new ArrayList<String>(Arrays.asList("", "CIVILIZATION", "REPEAT", ""));
            shapesSets.add(new ShapesSet(giveStrings, collectStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "FUTURE", "NO", "ATTACK"));
            collectStrings = new ArrayList<String>(Arrays.asList("", "", "NOT", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, collectStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("TOGETHER", "PURSUE", "SAFETY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));

            int giveTime = 200;
            int giveQs = 1;
            for (int i = 0; i < 9; i++) {
                if (i > 3) {
                    giveTime -= 10;
                }
                if (i == 2 || i == 3 || i == 6 || i == 8) {
                    giveQs++;
                }
                difficulty.add(i, new Difficulty(giveQs, giveTime));
            }

            int level = (int)(Math.random() * 9);
            defTime = difficulty.get(level).time;
            if (difficulty.get(level).qs > 1) {
                int randomVal = (int) (Math.random() * shapesSets.size());
                while (shapesSets.get(randomVal).strings.size() != difficulty.get(level).qs) {
                    randomVal = (int) (Math.random() * shapesSets.size());
                }
                qTotal = shapesSets.get(randomVal).strings.size();
                Log.v("echo", "qTotal:" + qTotal);
                throughList = new ThroughList[qTotal];
                answerThroughList = new ThroughList[qTotal];
                for (int i = 0; i < qTotal; i++) {
                    throughList[i] = new ThroughList();
                    answerThroughList[i] = shapes.get(shapesSets.get(randomVal).strings.get(i));
                /*
                int randomVal = (int)(Math.random()*counter);
                answerThroughList[i] = shapes.get("" + randomVal);
                */
                }
            } else {
                qTotal = 1;
                int randomVal = (int)(Math.random() * shapes.size());
                throughList = new ThroughList[qTotal];
                answerThroughList = new ThroughList[qTotal];
                throughList[0] = new ThroughList();
                answerThroughList[0] = shapes.get("" + randomVal);
            }

            display.getSize(point);
            offsetX = point.x / 2;
            offsetY = point.y / 2 + (point.y - point.x) / 5;
            radius = offsetX * 0.8;
            p.setAntiAlias(true);

            for (int i = 0; i < 11; i++) {
                dots[i] = new PointF();
            }

            dots[0].set(offsetX, offsetY);
            for (int i = 1; i < 5; i++) {
                int j = i;
                if(i > 1) {
                    j++;
                    if (i > 3) {
                        j++;
                    }
                }
                dots[i].set((float)(Math.cos(cr*(j-0.5)) * (radius / 2) + offsetX), (float) (Math.sin(cr * (j-0.5)) * (radius / 2) + offsetY));
            }
            for (int i = 5; i < 11; i++) {
                dots[i].set((float) (Math.cos(cr*(i-0.5)) * radius + offsetX), (float) (Math.sin(cr*(i-0.5)) * radius + offsetY));
            }

            for (int i = 0; i < 11; i++) {
                isThrough[i] = false;
            }

            Timer timer = new Timer(false);
            timer.schedule(new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            if (state) {
                                //再描画
                                invalidate();
                            }
                        }
                    });
                }
            }, 100, 25);
        }

        @Override
        public void onDraw(Canvas c) {
            c.drawColor(getResources().getColor(R.color.background));
            /*
            c.drawColor(Color.WHITE);

            p.setColor(Color.BLACK);
            p.setStyle(Paint.Style.FILL);
            Path path = new Path();
            path.moveTo((float) (Math.cos(cr*(-0.5)) * (offsetX / Math.cos(cr/2)) + offsetX), (float) (Math.sin(cr*(-0.5)) * (offsetX / Math.cos(cr/2)) + offsetY));
            for (int i = 1; i < 7; i++) {
                path.lineTo((float) (Math.cos(cr*(i-0.5)) * (offsetX / Math.cos(cr/2)) + offsetX), (float) (Math.sin(cr*(i-0.5)) * (offsetX / Math.cos(cr/2)) + offsetY));
            }
            c.drawPath(path, p);
            */

            float dotRadius = (float)radius/18;
            for (int i = 0; i < 11; i++) {
                int alpha = 0;
                for (int j = 0; j < 36; j++) {
                    if (j % 3 == 0) {
                        alpha++;
                    }
                    if(j >= 16 && j % 2 == 0) {
                        alpha++;
                    }
                    p.setColor(Color.argb(alpha, 150, 120, 150));
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(dots[i].x, dots[i].y, dotRadius + 4 + 36 - j, p);
                }
                p.setColor(getResources().getColor(R.color.dots));
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(dots[i].x, dots[i].y, dotRadius + 4, p);
                if (!isThrough[i]) {
                    p.setColor(getResources().getColor(R.color.background));
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(dots[i].x, dots[i].y, dotRadius, p);
                }
            }

            for (Point point: Locus) {
                p.setColor(Color.YELLOW);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(point.x, point.y, dotRadius / 4, p);
            }
            p.setColor(Color.RED);
            p.setStyle(Paint.Style.STROKE);
            c.drawPath(locusPath, p);

            if (!isStartGame) {
                showAnswer(0, framec);
            }

            if (isStartGame) {
                p.setTextSize(50);
                p.setColor(Color.WHITE);
                int leftTime = defTime - framec / 4;
                if (leftTime <= 0) {
                    doCount = false;
                    if (isFirstOnTimeup) {
                        for (int i = 0; i < qTotal; i++) {
                            Log.v("echo", "q[" + i + "]:" + judgeLocus(answerThroughList[i], throughList[i]));
                        }
                        isFirstOnTimeup = false;
                    }
                }
                c.drawText(leftTime / 10 + "." + leftTime % 10, offsetX, offsetY / 6, p);
            }

            if (doCount) {
                framec++;
            } else {
                float width = (qTotal - 1) * dotRadius * 6;
                for (int i = 0; i < qTotal; i++) {
                    p.setColor(Color.WHITE);
                    if (judgeLocus(answerThroughList[i], throughList[i])) {
                        p.setStyle(Paint.Style.FILL);
                    } else {
                        p.setStyle(Paint.Style.STROKE);
                    }
                    c.drawCircle(offsetX - width / 2 + i * dotRadius * 6, offsetY / 4, dotRadius * 3 / 2, p);
                }
            }
        }

        public void showAnswer(int initTime, int currentTime) {
            int interval = 30;
            int que = 0;
            for (int i = 0; i < (currentTime - initTime) / interval; i++) {
                que++;
            }
            if (!(que > (qTotal - 1) * 2 + 1)) {
                Log.v("echo", "do, que:" + que + ", initTime:" + initTime + ", currentTime:" + currentTime);
                if (que % 2 == 0) {
                    for (int i = 0; i < answerThroughList[que / 2].dots.size(); i++) {
                        if (i == 0) {
                            resetLocus();
                            setLocusStart(dots[answerThroughList[que / 2].dots.get(i)].x, dots[answerThroughList[que / 2].dots.get(i)].y, false);
                        } else {
                            setLocus(dots[answerThroughList[que / 2].dots.get(i)].x, dots[answerThroughList[que / 2].dots.get(i)].y, false);
                        }
                    }
                } else {
                    resetLocus();
                }
            }

            if (que > (qTotal - 1) * 2 + 1) {
                framec = 0;
                isStartGame = true;
            }
        }

        public boolean judgeLocus (ThroughList answer, ThroughList through) {
            ArrayList<int[]> answerPaths = new ArrayList<int[]>();
            ArrayList<int[]> passedPaths = new ArrayList<int[]>();

            if (answer.dots.size() != through.dots.size()) {
                return false;
            } else {
                for (int i = 0; i < answer.dots.size() - 1; i++) {
                    int[] path0 = {answer.dots.get(i), answer.dots.get(i + 1)};
                    answerPaths.add(path0);
                    int[] path1 = {through.dots.get(i), through.dots.get(i + 1)};
                    passedPaths.add(path1);
                }

                boolean[] clearFrags = new boolean[answerPaths.size()];
                for (int i = 0; i < answerPaths.size(); i++) {
                    for ( int j = 0; j < passedPaths.size(); j++) {
                        int[] tempPaths = {passedPaths.get(j)[1], passedPaths.get(j)[0]};
                        if (Arrays.equals(answerPaths.get(i), passedPaths.get(j)) || Arrays.equals(answerPaths.get(i), tempPaths)) {
                            clearFrags[i] = true;
                        }
                    }
                }
                //Log.v("echo", "clearC:"+clearC+", answerPaths.size():"+answerPaths.size());
                int clearC = 0;
                for (boolean flag: clearFrags) {
                    if (flag) {
                        clearC++;
                    }
                }
                return (clearC == answerPaths.size());
            }
        }

        public void setLocusStart(float x, float y, boolean doCD) {
            Locus.add(new Point((int)x, (int)y));
            if (doCD) {
                isCollision(x, y, x, y);
            }
            locusPath.moveTo(x, y);
        }

        public void setLocus(float x, float y, boolean doCD) {
            /*
            for (int i = 0; i < 3; i++){
                int blurR = (int)(Math.random() * offsetX * 0.8 / 15);
                double blurA = Math.random() * Math.PI * 2;

                Point locus = new Point((int)x + (int)(blurR * Math.cos(blurA)), (int)y + (int)(blurR * Math.sin(blurA)));
                Locus.add(locus);
            }
            */
            Locus.add(new Point((int)x, (int)y));
            if (doCD) {
                isCollision(x, y, Locus.get(Locus.size() - 2).x, Locus.get(Locus.size() - 2).y);
            }
            locusPath.lineTo(x, y);
        }

        public void isCollision(float x0, float y0, float x1, float y1) {
            int collisionDot = -1;
            for (int i = 0; i < 11; i++) {
                if(x0 == x1 && y0 == y1) {
                    //円の方程式にて当たり判定
                    if ((x0 - dots[i].x) * (x0 - dots[i].x) + (y0 - dots[i].y) * (y0 - dots[i].y) < (offsetX * 0.8 / 18 + 20) * (offsetX * 0.8 / 18 + 20) && state) {
                        isThrough[i] = true;
                        collisionDot = i;
                    }
                }
                //線分と円の当たり判定
                float a = y0 - y1, b = x1 - x0, c = x0 * y1 - x1 * y0;
                double d = (a * dots[i].x + b * dots[i].y + c) / Math.sqrt(a * a + b * b);
                double lim = offsetX * 0.8 / 18 + 30;
                if (-lim <= d && d <= lim) {    //線分への垂線と半径
                    double inner0 = x0 * dots[i].y - dots[i].x * y0, inner1 = x1 * dots[i].y - dots[i].x * y1;
                    double d0 = Math.sqrt((x0 - dots[i].x) * (x0 - dots[i].x) + (y0 - dots[i].y) * (y0 - dots[i].y));
                    double d1 = Math.sqrt((x1 - dots[i].x) * (x1 - dots[i].x) + (y1 - dots[i].y) * (y1 - dots[i].y));
                    if (inner0 * inner1 <= 0) { //内積
                        isThrough[i] = true;
                        collisionDot = i;
                    } else if (d0 < lim || d1 < lim) {
                        isThrough[i] = true;
                        collisionDot = i;
                    }
                }
            }
            if (collisionDot != -1 && (throughList[qNum].dots.size() < 1 || throughList[qNum].dots.get(throughList[qNum].dots.size() - 1) != collisionDot)) {
                throughList[qNum].dots.add(collisionDot);
            }
        }

        public void resetLocus() {
            locusPath.reset();
            Locus.clear();
        }

        public void resetThrough() {
            for (int i = 0; i < 11; i++) {
                isThrough[i] = false;
            }
        }

        float downX = 0, downY = 0;
        float memX = 0, memY = 0;
        boolean isReleased = false;
        public boolean onTouchEvent(MotionEvent event) {
            float upX = 0, upY = 0;
            float lim = 40;
            switch (event.getAction()) {
                //タッチ
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    if (doCount) {
                        if (isReleased) {
                            resetLocus();
                            resetThrough();
                        }
                        isReleased = false;
                        memX = downX;
                        memY = downY;
                        setLocusStart(downX, downY, true);
                    }
                    break;
                //スワイプ
                case MotionEvent.ACTION_MOVE:
                    float currentX = event.getX();
                    float currentY = event.getY();
                    if (doCount) {
                        //if (currentX + lim < memX || memX + lim < currentX || currentY + lim < memY || memY + lim < currentY) {
                            setLocus(currentX, currentY, true);
                            memX = currentX;
                            memY = currentY;
                        //}
                    }
                    break;
                //リリース
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    upX = event.getX();
                    upY = event.getY();
                    if (doCount) {
                        isReleased = true;
                        String list = "";
                        for (int throughDot : throughList[qNum].dots) {
                            list += throughDot + ",";
                        }
                        Log.v("echo", "throughList:" + list);
                        resetLocus();
                        boolean isFirst = true;
                        for (Integer integer : throughList[qNum].dots) {
                            if (isFirst) {
                                setLocusStart(dots[integer].x, dots[integer].y, false);
                                isFirst = false;
                            } else {
                                setLocus(dots[integer].x, dots[integer].y, false);
                            }
                        }
                        if (qTotal - 1 > qNum) {
                            qNum++;
                        } else {
                            doCount = false;
                            for (int i = 0; i < qTotal; i++) {
                                Log.v("echo", "q[" + i + "]:" + judgeLocus(answerThroughList[i], throughList[i]));
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    }
}
