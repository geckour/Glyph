package jp.org.geckour.glyph;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MyActivity extends Activity {
    SharedPreferences sp;
    int min = 0;
    int max = 8;
    MyView view;
    float offsetX, offsetY;
    boolean isFocused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            min = Integer.parseInt(sp.getString("min_level", "0"));
            Log.v("echo", "min:" + min);
        } catch (Exception e) {
            Log.v("error", "Can't translate minimum-level to int.");
        }
        try {
            max = Integer.parseInt(sp.getString("max_level", "8"));
            Log.v("echo", "max:" + max);
        } catch (Exception e) {
            Log.v("error", "Can't translate maximum-level to int.");
        }
        view = new MyView(this);
        setContentView(view);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        offsetX = view.getWidth() / 2;
        offsetY = view.getHeight() / 2;
        isFocused = true;
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

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0){ //Pref.javaからの戻り値の場合
            if (resultCode == Activity.RESULT_OK) {
                Log.v("echo", "level is changed.");
            }
        }
    }

    class MyView extends View {
        private final Handler handler = new Handler();
        boolean state = true;
        int gameMode;
        Paint p = new Paint();
        double cr = Math.PI / 3;
        double radius;
        PointF[] dots = new PointF[11];
        ArrayList<Point> Locus = new ArrayList<Point>();
        ArrayList<Point> blurLocus = new ArrayList<Point>();
        Path locusPath = new Path();
        int framec = 0;
        boolean[] isThrough = new boolean[11];
        ThroughList[] throughList;
        ThroughList[] answerThroughList;
        int qTotal = 0;
        int qNum = 0;
        int defTime = 200;
        int marginTime = 30;
        LinkedHashMap<String, ThroughList> shapes = new LinkedHashMap<String, ThroughList>();
        List shapesKeyList;
        ArrayList<ShapesSet> shapesSets = new ArrayList<ShapesSet>();
        boolean isEndLoad = false;
        boolean isFirstDraw = true;
        boolean isFirstTimeUp = true;
        boolean isFirstEndGame = true;
        ArrayList<Difficulty> difficulty = new ArrayList<Difficulty>();
        boolean isStartGame = false;
        boolean isEndGame = false;
        boolean doShow = true;
        Typeface typeface;
        ArrayList<String> correctStr;
        int holdTime;
        PointF[] buttonPoint = new PointF[2];

        public class ThroughList {
            ArrayList<Integer> dots;

            public ThroughList() {
                dots = new ArrayList<Integer>();
            }

            public ThroughList(ArrayList<Integer> argDots) {
                dots = new ArrayList<Integer>(argDots);
            }
        }

        public class ShapesSet {
            ArrayList<String> strings;
            ArrayList<String> correctStrings;

            public ShapesSet(ArrayList<String> argStrings) {
                strings = argStrings;
            }

            public ShapesSet(ArrayList<String> argStrings, ArrayList<String> argCorrectString) {
                strings = argStrings;
                correctStrings = argCorrectString;
            }

            public ArrayList<String> getCorrectStrings() {
                if (correctStrings != null) {
                    ArrayList<String> tempStrings = new ArrayList<String>();
                    for (int i = 0; i < correctStrings.size(); i++) {
                        if (correctStrings.get(i).equals("")) {
                            tempStrings.add(strings.get(i));
                        } else {
                            tempStrings.add(correctStrings.get(i));
                        }
                    }
                    return tempStrings;
                } else {
                    return strings;
                }
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
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 2, 9, 8));
            shapes.put("ABANDON", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 1));
            shapes.put("ADAPT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3, 9));
            shapes.put("ADVANCE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 0, 4, 1));
            shapes.put("AGAIN", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 7, 8, 9, 10, 5));
            shapes.put("ALL", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1, 0));
            shapes.put("ANSWER", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 5, 4, 7));
            shapes.put("ATTACK", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 5, 4, 6, 1));
            shapes.put("AVOID", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 1, 7));
            shapes.put("BARRIER", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 2, 8, 1));
            shapes.put("BEGIN", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 3, 4, 1, 8));
            shapes.put("BEING", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 3, 4, 0));
            shapes.put("BODY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 4, 6));
            shapes.put("BREATHE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 1, 0, 2, 9, 8));
            shapes.put("CAPTURE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 8, 1));
            shapes.put("CHANGE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 10, 5, 6, 4, 0, 2, 8));
            shapes.put("CHAOS", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 8));
            shapes.put("CLEAR", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 8, 9, 10, 5, 6, 7, 8));
            shapes.put("CLEAR ALL", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 0, 2));
            shapes.put("COMPLEX", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 1, 4, 7));
            shapes.put("CONFLICT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 1, 7));
            shapes.put("CONSEQUENCE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 7, 8, 2, 3, 0, 4));
            shapes.put("CONTEMPLATE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 4, 7));
            shapes.put("CONTRACT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 1));
            shapes.put("COURAGE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 6));
            shapes.put("CREATE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 8, 2, 10, 3, 0));
            shapes.put("CREATIVITY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 9, 10, 3, 0, 1, 7, 6, 4));
            shapes.put("MIND", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3, 0, 8));
            shapes.put("DANGER", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 0, 2, 8));
            shapes.put("DATA", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 8, 1, 6));
            shapes.put("DEFEND", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 7, 8));
            shapes.put("DESTINATION", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 4, 1, 2, 8));
            shapes.put("DESTINY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 1, 7));
            shapes.put("DESTROY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 2, 9));
            shapes.put("DETERIORATE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 1, 7));
            shapes.put("DIE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1, 4, 6));
            shapes.put("DIFFICULT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 7, 8, 9));
            shapes.put("DISCOVER", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 10, 9));
            shapes.put("DISTANCE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 0, 2, 8));
            shapes.put("EASY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 5, 6, 1, 8));
            shapes.put("END", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 7, 6, 5, 3, 0, 4, 3));
            shapes.put("ENLIGHTENED_A", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 8, 7, 6, 5, 3, 0, 4, 3));
            shapes.put("ENLIGHTENED_B", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1));
            shapes.put("EQUAL", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 4, 3, 2));
            shapes.put("ESCAPE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 3, 2));
            shapes.put("EVOLUTION", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 4, 1));
            shapes.put("FAILURE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1, 6));
            shapes.put("FEAR", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 6, 7));
            shapes.put("FOLLOW", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 9));
            shapes.put("FORGET", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 1, 7));
            shapes.put("FUTURE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2));
            shapes.put("GAIN", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 1, 4, 6));
            shapes.put("GOVERNMENT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2));
            shapes.put("GROW", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(7, 1, 0, 3, 5, 4, 0));
            shapes.put("HARM", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 1, 8, 2, 0, 4, 5, 3, 0));
            shapes.put("HARMONY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 0, 2, 8));
            shapes.put("HAVE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 2, 1));
            shapes.put("HELP", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 5, 6, 1, 8));
            shapes.put("END", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 6, 1, 2));
            shapes.put("HIDE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 4, 8));
            shapes.put("I", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 7));
            shapes.put("IGNORE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 0, 2, 3, 0));
            shapes.put("IMPERFECT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 1));
            shapes.put("IMPROVE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 3, 2, 0));
            shapes.put("IMPURE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 3, 10, 9, 2, 0, 8));
            shapes.put("INTERRUPT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 3, 10, 9, 8));
            shapes.put("JOURNEY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 0, 4, 8));
            shapes.put("KNOWLEDGE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 10, 9, 2, 8));
            shapes.put("LEAD", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 10, 5, 6, 4, 1, 7));
            shapes.put("LEGACY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 4));
            shapes.put("LESS", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 0, 4, 6, 5));
            shapes.put("LIBERATE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 0, 1, 4, 0));
            shapes.put("LIE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 0, 4, 6));
            shapes.put("LIVE AGAIN", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 1));
            shapes.put("LOSE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 0, 1, 6));
            shapes.put("MESSAGE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 3, 0, 8));
            shapes.put("IDEA", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1));
            shapes.put("MORE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 5, 4, 3, 2));
            shapes.put("MYSTERY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 4, 1, 7));
            shapes.put("NATURE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 1, 7));
            shapes.put("NEW", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1));
            shapes.put("NO", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 9, 2, 0, 8));
            shapes.put("NOURISH", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2));
            shapes.put("OLD", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 1, 8));
            shapes.put("OPEN", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 1, 8, 9, 10, 5, 6, 7, 8));
            shapes.put("OPEN ALL", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 4, 6, 7, 1, 2, 9, 10));
            shapes.put("OPENING", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 9));
            shapes.put("PAST", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 2, 9));
            shapes.put("PATH", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 2, 9, 8, 7, 1, 0));
            shapes.put("PERFECTION", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 5, 3, 0, 1, 7));
            shapes.put("PERSPECTIVE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 1, 7, 6));
            shapes.put("POTENTIAL", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 0, 4, 1, 2, 8, 1));
            shapes.put("PRESENCE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 2, 1, 4));
            shapes.put("PRESENT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 4, 1, 0));
            shapes.put("PURE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 5, 4));
            shapes.put("PURSUE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 3, 2, 9));
            shapes.put("CHASE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 3, 2));
            shapes.put("QUESTION", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 0, 1, 7));
            shapes.put("REACT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 4, 6, 7));
            shapes.put("REBEL", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 3, 10, 5, 0));
            shapes.put("RECHARGE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 5, 0, 8, 2));
            shapes.put("RESISTANCE_A", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 5, 0, 8, 1));
            shapes.put("RESISTANCE_B", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 1, 7, 8));
            shapes.put("RESTRAINT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 7));
            shapes.put("RETREAT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 4, 7));
            shapes.put("SAFETY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1, 6));
            shapes.put("SAVE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3));
            shapes.put("SEE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 4, 3, 2, 1));
            shapes.put("SEEK", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 8, 7));
            shapes.put("SELF", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 0, 4, 1, 7));
            shapes.put("SEPARATE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 5, 4, 1, 7));
            shapes.put("SHAPERS", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(7, 1, 2, 9, 8));
            shapes.put("SHARE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 1));
            shapes.put("SIMPLE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 4, 1, 8));
            shapes.put("SOUL", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 1, 7));
            shapes.put("STABILITY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1, 2));
            shapes.put("STRONG", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 3, 0));
            shapes.put("TOGETHER", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 1, 4, 0, 2, 3));
            shapes.put("TRUTH", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 1, 6));
            shapes.put("USE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 5, 4, 8));
            shapes.put("VICTORY", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 8, 1));
            shapes.put("WANT", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 8));
            shapes.put("WE", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 4, 1));
            shapes.put("WEAK", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 1, 6));
            shapes.put("WORTH", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1, 0, 2));
            shapes.put("XM", new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 2, 1, 5));
            shapes.put("YOU", new ThroughList(giveDot));

            shapesKeyList = new ArrayList<String>(shapes.keySet());

            ArrayList<String> giveStrings;
            ArrayList<String> correctStrings;
            //#5
            giveStrings = new ArrayList<String>(Arrays.asList("BREATHE", "NO", "XM", "LOSE", "SELF"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "INSIDE", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CLEAR", "MIND", "LIBERATE", "BARRIER", "BODY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "PURE", "FUTURE", "BEING", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "HUMAN", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "PURE", "FUTURE", "NO", "ATTACK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "NOT", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DEFEND", "BEING", "GOVERNMENT", "SHAPERS", "LIE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "CIVILIZATION", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DESTROY", "BEING", "GOVERNMENT", "SHAPERS", "LIE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "CIVILIZATION", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DESTROY", "GOVERNMENT", "END", "CONFLICT", "ATTACK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "CIVILIZATION", "", "", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISTANCE", "I", "AVOID", "BEING", "LIE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "SELF", "", "HUMAN", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("FORGET", "PAST", "SEE", "PRESENT", "DANGER"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("FORGET", "ATTACK", "SEE", "DISTANCE", "HARMONY"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "WAR", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("BEING", "SHAPERS", "TOGETHER", "CREATE", "DESTINY"));
            correctStrings = new ArrayList<String>(Arrays.asList("HUMAN", "", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HARM", "EVOLUTION", "PURSUE", "MORE", "ATTACK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PROGRESS", "", "", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("MORE", "DATA", "GAIN", "OPENING", "ADVANCE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "PORTAL", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISTANCE", "SELF", "AVOID", "BEING", "LIE"));
            correctStrings = new ArrayList<String>(Arrays.asList("OUTSIDE", "", "", "HUMAN", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURSUE", "CONFLICT", "ATTACK", "ADVANCE", "CHAOS"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "WAR", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SAVE", "BEING", "GOVERNMENT", "DESTROY", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "CIVILIZATION", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEPARATE", "MIND", "BODY", "DISCOVER", "ENLIGHTENED_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "", "ENLIGHTENMENT"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEPARATE", "TRUTH", "LIE", "SHAPERS", "FUTURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SHAPERS", "OPENING", "DATA", "CREATE", "CHAOS"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PORTAL", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SHAPERS", "WANT", "BEING", "MIND", "FUTURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "HUMAN", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SIMPLE", "TRUTH", "FORGET", "EASY", "EVOLUTION"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "", "SUCCESS"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("STABILITY", "STRONG", "TOGETHER", "DEFEND", "RESISTANCE_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("STAY", "", "", "", "RESISTANCE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("USE", "RESTRAINT", "FOLLOW", "EASY", "PATH"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("WANT", "TRUTH", "PURSUE", "DIFFICULT", "PATH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("WEAK", "BEING", "DESTINY", "DESTROY", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "", "", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            //#4
            giveStrings = new ArrayList<String>(Arrays.asList("ADVANCE", "GOVERNMENT", "AGAIN", "FAILURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "CIVILIZATION", "REPEAT", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ATTACK", "ENLIGHTENED_A", "PURSUE", "RESISTANCE_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "ENLIGHTENMENT", "", "RESISTANCE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ATTACK", "RESISTANCE_A", "PURSUE", "ENLIGHTENED_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "RESISTANCE", "", "ENLIGHTENMENT"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ATTACK", "WEAK", "SHAPERS", "LIE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AVOID", "XM", "MESSAGE", "LIE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("BREATHE", "AGAIN", "JOURNEY", "AGAIN"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("BREATHE", "NATURE", "PERFECTION", "HARMONY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CAPTURE", "FEAR", "DISCOVER", "COURAGE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CHANGE", "BODY", "IMPROVE", "BEING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "HUMAN"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CHANGE", "FUTURE", "CAPTURE", "DESTINY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CHANGE", "BEING", "POTENTIAL", "USE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CHAOS", "BARRIER", "SHAPERS", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CHAOS", "DESTROY", "SHAPERS", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CLEAR", "MIND", "OPEN", "MIND"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CLEAR ALL", "OPEN ALL", "DISCOVER", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("COMPLEX", "SHAPERS", "GOVERNMENT", "STRONG"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "CIVILIZATION", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CONTEMPLATE", "COMPLEX", "SHAPERS", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CONTEMPLATE", "COMPLEX", "SHAPERS", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CONTEMPLATE", "SELF", "PATH", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("COURAGE", "ATTACK", "SHAPERS", "FUTURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "WAR", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "DISTANCE", "IMPURE", "PATH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "FUTURE", "CHANGE", "DESTINY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "FUTURE", "NO", "ATTACK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "INSIDE", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "FUTURE", "NO", "ATTACK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "NOT", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DEFEND", "MESSAGE", "ANSWER", "MIND"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "IDEA"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DESTROY", "COMPLEX", "SHAPERS", "LIE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DESTROY", "DESTINY", "BEING", "LIE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "HUMAN", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DETERIORATE", "BEING", "WEAK", "REBEL"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("END", "JOURNEY", "DISCOVER", "DESTINY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ESCAPE", "SIMPLE", "BEING", "FUTURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "HUMAN", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("FOLLOW", "SHAPERS", "OPENING", "MESSAGE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "PORTAL", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("FORGET", "CONFLICT", "OPEN", "ATTACK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "ACCEPT", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("GAIN", "OPENING", "ATTACK", "WEAK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PORTAL", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HELP", "GAIN", "CREATE", "PURSUE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HELP", "SHAPERS", "CREATE", "FUTURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HIDE", "IMPURE", "BEING", "MIND"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "HUMAN", "THOUGHT"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("BEING", "PAST", "PRESENT", "FUTURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("HUMAN", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("BEING", "SOUL", "STRONG", "PURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("HUMAN", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("IGNORE", "BEING", "CHAOS", "LIE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("IMPROVE", "BODY", "PURSUE", "JOURNEY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("IMPROVE", "MIND", "JOURNEY", "NO"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "INSIDE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("NO", "MIND", "JOURNEY", "PERFECTION"));
            correctStrings = new ArrayList<String>(Arrays.asList("INSIDE", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("JOURNEY", "NO", "IMPROVE", "SOUL"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "INSIDE", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LEAD", "PURSUE", "REACT", "DEFEND"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LESS", "MIND", "MORE", "SOUL"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LESS", "TRUTH", "MORE", "CHAOS"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LIBERATE", "XM", "OPENING", "TOGETHER"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "PORTAL", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LOSE", "DANGER", "GAIN", "SAFETY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("MORE", "MIND", "LESS", "SOUL"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "SPIRIT"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("NOURISH", "XM", "CREATE", "MIND"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "THOUGHT"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PAST", "AGAIN", "PRESENT", "AGAIN"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PATH", "RESTRAINT", "STRONG", "SAFETY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HARMONY", "PATH", "NOURISH", "PRESENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("PEACE", "", "", "NOW"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PERFECTION", "PERFECTION", "SAFETY", "ALL"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "BALANCE", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("OPENING", "CHANGE", "GOVERNMENT", "END"));
            correctStrings = new ArrayList<String>(Arrays.asList("PORTAL", "", "CIVILIZATION", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("OPENING", "DIE", "GOVERNMENT", "DIE"));
            correctStrings = new ArrayList<String>(Arrays.asList("PORTAL", "", "CIVILIZATION", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("OPENING", "HAVE", "TRUTH", "DATA"));
            correctStrings = new ArrayList<String>(Arrays.asList("PORTAL", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("OPENING", "POTENTIAL", "CHANGE", "FUTURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("PORTAL", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("QUESTION", "TRUTH", "GAIN", "FUTURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("RESTRAINT", "FEAR", "AVOID", "DANGER"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("RESTRAINT", "PATH", "GAIN", "HARMONY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEEK", "DATA", "DISCOVER", "PATH"));
            correctStrings = new ArrayList<String>(Arrays.asList("SEARCH", "", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEEK", "TRUTH", "SAVE", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("SEARCH", "", "", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEEK", "XM", "SAVE", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("SEARCH", "", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEPARATE", "WEAK", "IGNORE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SHAPERS", "CHAOS", "PURE", "HARM"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SHAPERS", "HAVE", "STRONG", "PATH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SHAPERS", "MESSAGE", "END", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SHAPERS", "MIND", "COMPLEX", "HARMONY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SHAPERS", "PAST", "PRESENT", "FUTURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SHAPERS", "OPENING", "MIND", "RESTRAINT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PORTAL", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SIMPLE", "GOVERNMENT", "IMPURE", "WEAK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "CIVILIZATION", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SIMPLE", "MESSAGE", "COMPLEX", "IDEA"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SOUL", "BEING", "REBEL", "DIE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("STABILITY", "TOGETHER", "DEFEND", "TRUTH"));
            correctStrings = new ArrayList<String>(Arrays.asList("STAY", "HUMAN", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("STRONG", "MIND", "PURSUE", "TRUTH"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "IDEA", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("STRONG", "RESISTANCE_A", "CAPTURE", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "RESISTANCE", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("STRONG", "TOGETHER", "AVOID", "ATTACK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AVOID", "IMPROVE", "BEING", "SOUL"));
            correctStrings = new ArrayList<String>(Arrays.asList("STRUGGLE", "", "HUMAN", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("TOGETHER", "DISCOVER", "HARMONY", "EQUAL"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("TRUTH", "MIND", "DISCOVER", "XM"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "IDEA", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("XM", "DIE", "CHAOS", "BREATHE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "", "LIVE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("XM", "HAVE", "MIND", "JOURNEY"));
            shapesSets.add(new ShapesSet(giveStrings));
            //#3
            giveStrings = new ArrayList<String>(Arrays.asList("OPEN", "BEING", "WEAK"));
            correctStrings = new ArrayList<String>(Arrays.asList("ACCEPT", "HUMAN", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ADVANCE", "BEING", "ENLIGHTENED_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "ENLIGHTENED"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ADVANCE", "BEING", "RESISTANCE_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", "RESISTANCE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ADVANCE", "PURE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AGAIN", "JOURNEY", "DISTANCE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "OUTSIDE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ALL", "GOVERNMENT", "CHAOS"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "CIVILIZATION", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ATTACK", "DIFFICULT", "FUTURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ATTACK", "SHAPERS", "EVOLUTION"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AVOID", "CHAOS", "SOUL"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AVOID", "COMPLEX", "CONFLICT"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AVOID", "COMPLEX", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AVOID", "DESTINY", "LIE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AVOID", "PURE", "CHAOS"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AVOID", "ATTACK", "CHAOS"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "WAR", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ANSWER", "AGAIN", "AVOID"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "REPEAT", "STRUGGLE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CAPTURE", "SHAPERS", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CAPTURE", "XM", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("COMPLEX", "JOURNEY", "FUTURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CONTEMPLATE", "JOURNEY", "DISTANCE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "OUTSIDE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CONTEMPLATE", "POTENTIAL", "PERFECTION"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("COURAGE", "DESTINY", "REBEL"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DESTROY", "GOVERNMENT", "DANGER"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "CIVILIZATION", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DESTROY", "IMPURE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DESTROY", "WEAK", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "OPENING", "TRUTH"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PORTAL", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "PURE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "RESISTANCE_A", "TRUTH"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "RESISTANCE", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "SAFETY", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "SHAPERS", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "SHAPERS", "ENLIGHTENED_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "ENLIGHTENMENT"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "SHAPERS", "LIE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "SHAPERS", "MESSAGE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ESCAPE", "IMPURE", "EVOLUTION"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ESCAPE", "IMPURE", "FUTURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ESCAPE", "SHAPERS", "HARM"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("FEAR", "CHAOS", "XM"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("FOLLOW", "PURE", "JOURNEY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("FUTURE", "EQUAL", "PAST"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("GAIN", "GOVERNMENT", "HARMONY"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "CIVILIZATION", "PEACE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("GAIN", "FUTURE", "ESCAPE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HARM", "DANGER", "AVOID"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HIDE", "JOURNEY", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("BEING", "GAIN", "SAFETY"));
            correctStrings = new ArrayList<String>(Arrays.asList("HUMAN", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("IMPROVE", "ADVANCE", "PRESENT"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("IMPROVE", "FUTURE", "TOGETHER"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("IMPROVE", "BEING", "SHAPERS"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("NO", "MIND", "FUTURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("INSIDE", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("NO", "XM", "TRUTH"));
            correctStrings = new ArrayList<String>(Arrays.asList("INSIDE", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LEAD", "ENLIGHTENED_A", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "ENLIGHTENMENT", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LEAD", "RESISTANCE_A", "QUESTION"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "RESISTANCE", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LIBERATE", "OPENING", "POTENTIAL"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PORTAL", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LOSE", "ATTACK", "RETREAT"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("MIND", "EQUAL", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("MIND", "OPEN", "BREATHE"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "LIVE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("NATURE", "PURE", "DEFEND"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("NOURISH", "MIND", "JOURNEY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("NOURISH", "XM", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("OPEN ALL", "OPENING", "EVOLUTION"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PORTAL", "SUCCESS"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("OPEN ALL", "SIMPLE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PATH", "HARMONY", "DIFFICULT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PEACE", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HARMONY", "SIMPLE", "JOURNEY"));
            correctStrings = new ArrayList<String>(Arrays.asList("PEACE", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HARMONY", "STABILITY", "FUTURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("PEACE", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("POTENTIAL", "XM", "ATTACK"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("POTENTIAL", "XM", "HARMONY"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "PEACE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURSUE", "COMPLEX", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("QUESTION", "CONFLICT", "DATA"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("QUESTION", "HIDE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("REACT", "IMPURE", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("REPEAT", "SEARCH", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("AGAIN", "SEEK", "SAFETY"));
            correctStrings = new ArrayList<String>(Arrays.asList("REPEAT", "SEARCH", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEEK", "XM", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("SEARCH", "", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEE", "TRUTH", "PRESENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "NOW"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEPARATE", "FUTURE", "EVOLUTION"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("TOGETHER", "PURE", "JOURNEY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("TOGETHER", "PURSUE", "SAFETY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("TRUTH", "NOURISH", "SOUL"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("WANT", "TRUTH", "PRESENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "NOW"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ATTACK", "CREATE", "DANGER"));
            correctStrings = new ArrayList<String>(Arrays.asList("WAR", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ATTACK", "DESTROY", "FUTURE"));
            correctStrings = new ArrayList<String>(Arrays.asList("WAR", "", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("XM", "NOURISH", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            //#2
            giveStrings = new ArrayList<String>(Arrays.asList("ATTACK", "EVOLUTION"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CAPTURE", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CHANGE", "PRESENT"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("GOVERNMENT", "CHAOS"));
            correctStrings = new ArrayList<String>(Arrays.asList("CIVILIZATION", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("GOVERNMENT", "WEAK"));
            correctStrings = new ArrayList<String>(Arrays.asList("CIVILIZATION", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "DANGER"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("CREATE", "FUTURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DEFEND", "NATURE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DIFFICULT", "BARRIER"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "ENLIGHTENED_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "ENLIGHTENMENT"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "LIE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "OPENING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PORTAL"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("DISCOVER", "RESISTANCE_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "RESISTANCE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("ESCAPE", "EVOLUTION"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("FOLLOW", "JOURNEY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("GAIN", "HARMONY"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "PEACE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("GAIN", "SAFETY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HIDE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("IMPROVE", "BEING"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "HUMAN"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("JOURNEY", "NO"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "INSIDE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LEAD", "RESISTANCE_A"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "RESISTANCE"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("LIBERATE", "XM"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("OPEN ALL", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("HARMONY", "STABILITY"));
            correctStrings = new ArrayList<String>(Arrays.asList("PEACE", ""));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURE", "CHAOS"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURE", "LIE"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURE", "MIND"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURE", "SHAPERS"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURSUE", "CONFLICT"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("PURSUE", "JOURNEY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("QUESTION", "ALL"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("QUESTION", "GOVERNMENT"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "CIVILIZATION"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("RETREAT", "SAFETY"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEE", "SHAPERS"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEE", "TRUTH"));
            shapesSets.add(new ShapesSet(giveStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("SEPARATE", "ATTACK"));
            correctStrings = new ArrayList<String>(Arrays.asList("", "WAR"));
            shapesSets.add(new ShapesSet(giveStrings, correctStrings));
            giveStrings = new ArrayList<String>(Arrays.asList("STRONG", "BODY"));
            shapesSets.add(new ShapesSet(giveStrings));

            isEndLoad = true;

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
            gameMode = Integer.parseInt(sp.getString("gamemode", "0"));
            int level = (int) (Math.random() * (max - min + 1) + min);
            //int level = 8;
            defTime = difficulty.get(level).time;
            if (difficulty.get(level).qs > 1) {
                int randomVal = (int) (Math.random() * shapesSets.size());
                //int randomVal = 0;
                while (shapesSets.get(randomVal).strings.size() != difficulty.get(level).qs) {
                    randomVal = (int) (Math.random() * shapesSets.size());
                }
                qTotal = shapesSets.get(randomVal).strings.size();
                //Log.v("echo", "qTotal:" + qTotal);
                throughList = new ThroughList[qTotal];
                answerThroughList = new ThroughList[qTotal];
                for (int i = 0; i < qTotal; i++) {
                    throughList[i] = new ThroughList();
                    answerThroughList[i] = shapes.get(shapesSets.get(randomVal).strings.get(i));
                }
                correctStr = shapesSets.get(randomVal).getCorrectStrings();
                Log.v("echo", "randomVal:" + randomVal + ", level:" + level);
            } else {
                qTotal = 1;
                //Log.v("echo", "qTotal:" + qTotal);
                int randomVal = (int) (Math.random() * shapes.size());
                throughList = new ThroughList[qTotal];
                answerThroughList = new ThroughList[qTotal];
                throughList[0] = new ThroughList();
                answerThroughList[0] = shapes.get(shapesKeyList.get(randomVal).toString());
                correctStr = new ArrayList<String>(Arrays.asList(shapesKeyList.get(randomVal).toString()));
                //for debug of shapes
                //answerThroughList[0] = shapes.get("");
                //correctStr = new ArrayList<String>(Arrays.asList(""));
                Log.v("echo", "randomVal:" + randomVal + ", level:" + level);
            }
            p.setAntiAlias(true);

            for (int i = 0; i < 11; i++) {
                dots[i] = new PointF();
            }

            for (int i = 0; i < 11; i++) {
                isThrough[i] = false;
            }

            typeface = Typeface.createFromAsset(getContext().getAssets(), "Ricty-Regular.ttf");

            Timer timer = new Timer(false);
            timer.schedule(new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            if (state) {
                                invalidate(); //再描画
                            }
                        }
                    });
                }
            }, 100, 25);
        }

        @Override
        public void onDraw(Canvas c) {
            c.drawColor(getResources().getColor(R.color.background));

            if (isEndLoad) {
                if (isFirstDraw) {
                    radius = offsetX * 0.8;
                    dots[0].set(offsetX, (float) (offsetY * 1.2));
                    for (int i = 1; i < 5; i++) {
                        int j = i;
                        if (i > 1) {
                            j++;
                            if (i > 3) {
                                j++;
                            }
                        }
                        dots[i].set((float) (Math.cos(cr * (j - 0.5)) * (radius / 2) + offsetX), (float) (Math.sin(cr * (j - 0.5)) * (radius / 2) + offsetY * 1.2));
                    }
                    for (int i = 5; i < 11; i++) {
                        dots[i].set((float) (Math.cos(cr * (i - 0.5)) * radius + offsetX), (float) (Math.sin(cr * (i - 0.5)) * radius + offsetY * 1.2));
                    }
                    isFirstDraw = false;
                }

                if (!isStartGame) {
                    showAnswer(c, 0, framec);
                }
                if (isEndGame) {
                    if (isFirstEndGame) {
                        holdTime = framec;
                        isFirstEndGame = false;
                    }
                    if (framec > holdTime + marginTime) {
                        doShow = false;
                        showResult(c, marginTime, holdTime + marginTime, framec);
                    }
                }

                float dotRadius = (float) radius / 18;
                if (doShow) {
                    for (int i = 0; i < 11; i++) {
                        int alpha = 0;
                        for (int j = 0; j < 36; j++) {
                            if (j % 3 == 0) {
                                alpha++;
                            }
                            if (j >= 16 && j % 2 == 0) {
                                alpha++;
                            }
                            if (isThrough[i]) {
                                p.setColor(Color.argb(alpha, 220, 175, 50));
                            } else {
                                p.setColor(Color.argb(alpha, 150, 120, 150));
                            }
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
                    for (Point point : Locus) {
                        p.setColor(Color.rgb(255, 255, 150));
                        p.setStyle(Paint.Style.FILL);
                        c.drawCircle(point.x, point.y, dotRadius / 3, p);
                    }
                    if (!isStartGame || isReleased) {
                        p.setColor(Color.WHITE);
                        p.setStrokeWidth(3);
                        p.setStyle(Paint.Style.STROKE);
                        c.drawPath(locusPath, p);
                        p.setStrokeWidth(0);
                    }
                }

                if (isStartGame && doShow) {
                    showTime(c, framec);
                    showQueNumber(c, framec, 0, Color.argb(50, 0x02, 0xff, 0xc5), Color.argb(100, 0x02, 0xff, 0xc5));
                    showButton(c);
                } else if (doShow) {
                    showQueNumber(c, framec, marginTime, Color.argb(50, 220, 175, 50), Color.argb(100, 220, 175, 50));
                }

                framec++;
            }
        }

        public void showButton(Canvas c) {
            float buttonWidth = doShow ? 250 : 180;
            float buttonHeight = 80;
            float margin = 20;
            buttonPoint[0] = new PointF(offsetX * 2 - buttonWidth - margin, offsetY * 2 - buttonHeight - margin);
            buttonPoint[1] = new PointF(offsetX * 2 - margin, offsetY * 2 - margin);
            p.setColor(getResources().getColor(R.color.button));
            p.setStyle(Paint.Style.FILL);
            c.drawRect(buttonPoint[0].x, buttonPoint[0].y, buttonPoint[1].x, buttonPoint[1].y, p);

            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(70);
            if (doShow) {
                c.drawText("BYPASS", buttonPoint[0].x + buttonWidth / 2, buttonPoint[1].y - 20, p);
            } else {
                c.drawText("NEXT", buttonPoint[0].x + buttonWidth / 2, buttonPoint[1].y - 20, p);
            }
        }

        public Path makeHexagon(PointF origin, float r) {
            Path path = new Path();

            for (int i = 0; i < 7; i++) {
                if (i == 0) {
                    path.moveTo((float) (Math.cos(cr * (i - 0.5)) * r + origin.x), (float) (Math.sin(cr * (i - 0.5)) * r + origin.y));
                } else {
                    path.lineTo((float) (Math.cos(cr * (i - 0.5)) * r + origin.x), (float) (Math.sin(cr * (i - 0.5)) * r + origin.y));
                }
            }

            return path;
        }

        public void showTime(Canvas c, int currentTime) {
            p.setColor(Color.rgb(220, 190, 50));
            int leftTime = defTime - (isEndGame ? holdTime : currentTime) / 4;
            if (leftTime <= 0) {
                isEndGame = true;
                if (isFirstTimeUp) {
                    for (int i = 0; i < qTotal; i++) {
                        Log.v("echo", "q[" + i + "]:" + judgeLocus(answerThroughList[i], throughList[i]));
                    }
                    isFirstTimeUp = false;
                }
            }
            c.drawText(String.format("%02d", leftTime / 10) + ":" + leftTime % 10, offsetX, offsetY / 3, p);
            float barWidth = (float)(offsetX * 0.7 / defTime) * leftTime;
            p.setStyle(Paint.Style.FILL);
            c.drawRect(offsetX - barWidth, (float)(offsetY / 2.7), offsetX + barWidth, (float)(offsetY / 2.55), p);
        }

        public void showQueNumber(Canvas c, int currentTime, int marginTime, int normalColor, int strongColor) {
            float hexaRadius = offsetX / 10;
            float hexaMargin = 5;
            float totalMargin = hexaMargin * (qTotal - 1);
            float width = (qTotal - 1) * (offsetX / 5);
            float x, y;
            for (int i = 0; i < qTotal; i++) {
                x = offsetX - (width / 2 + totalMargin) + i * (hexaRadius + hexaMargin) * 2;
                y = (float)(offsetY / 7.5);
                PointF giveOrigin = new PointF(x, y);

                if (isStartGame) {
                    if (i == qNum) {
                        if ((isReleased && throughList[qTotal - 1].dots.size() > 0)) {
                            p.setColor(normalColor);
                            p.setStyle(Paint.Style.FILL);
                            c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
                        } else {
                            p.setColor(strongColor);
                            p.setStyle(Paint.Style.FILL);
                            c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
                        }
                    } else if (i < qNum) {
                        p.setColor(normalColor);
                        p.setStyle(Paint.Style.FILL);
                        c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
                    } else {
                        p.setColor(Color.BLACK);
                        p.setStyle(Paint.Style.FILL);
                        c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
                    }
                } else {
                    if (i == (currentTime - marginTime) / 50 && currentTime > marginTime) {
                        p.setColor(strongColor);
                        p.setStyle(Paint.Style.FILL);
                        c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
                    } else {
                        p.setColor(Color.BLACK);
                        p.setStyle(Paint.Style.FILL);
                        c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
                    }
                }
                p.setStrokeJoin(Paint.Join.BEVEL);
                p.setColor(Color.argb(255, Color.red(normalColor), Color.green(normalColor), Color.blue(normalColor)));
                p.setStyle(Paint.Style.STROKE);
                c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
            }
        }

        public void showAnswer(Canvas c, int initTime, int currentTime) {
            int showLength = 49;
            int hideLength = 1;
            int que = -1;

            if (currentTime - initTime > marginTime) {
                que++;
            }
            if (currentTime - initTime - marginTime >= 0) {
                que = (currentTime - initTime - marginTime) * 2 / (showLength + hideLength);
            }
            if ((currentTime - initTime - marginTime) % 2 >= showLength) {
                que++;
            }
            if (currentTime - initTime >= (qTotal + 2.2) * (showLength + hideLength) + marginTime) {
                que++;
            }
            switch (que - (qTotal * 2 - 1)) {
                default:
                    if (que % 2 == 0 && que >= 0) {
                        for (int i = 0; i < answerThroughList[que / 2].dots.size(); i++) {
                            if (gameMode == 0 || gameMode == 2) {
                                if (i == 0) {
                                    resetLocus();
                                    setLocusStart(dots[answerThroughList[que / 2].dots.get(i)].x, dots[answerThroughList[que / 2].dots.get(i)].y, false);
                                } else {
                                    setLocus(dots[answerThroughList[que / 2].dots.get(i)].x, dots[answerThroughList[que / 2].dots.get(i)].y, false);
                                }
                            }
                            if (gameMode == 0 || gameMode == 1) {
                                p.setColor(Color.WHITE);
                                p.setTextSize(80);
                                p.setTypeface(typeface);
                                p.setTextAlign(Paint.Align.CENTER);
                                c.drawText(correctStr.get(que / 2), offsetX, offsetY / 3, p);
                            }
                        }
                    } else {
                        resetLocus();
                    }
                    break;

                case 1:
                case 2:
                case 3:
                case 4:
                    showFlash(c, qTotal * (showLength + hideLength) + marginTime, currentTime, 35);
                    break;
            }
        }

        public void showFlash(Canvas c, int initTime, int currentTime, int interval) {
            int que;
            int margin = interval / 20;
            int diffTime = currentTime - initTime;
            int alpha = 255;

            que = (diffTime) / interval;
            if (diffTime > interval * 2.2) {
                que++;
            }

            if (que == 0) {
                if (diffTime < margin) {
                    alpha = 150 * diffTime / margin;
                } else {
                    alpha = 150 - 150 * diffTime / interval;
                }
            }
            if (que == 1) {
                if (diffTime < margin) {
                    alpha = 200 * diffTime / margin;
                } else {
                    alpha = 200 - 200 * diffTime / interval;
                }
            }
            p.setColor(Color.argb(alpha, 220, 175, 50));
            if (que == 2) {
                if (diffTime < margin) {
                    alpha = 255 * diffTime / margin;
                } else {
                    alpha = 255;
                }
                p.setColor(Color.argb(alpha, 255, 255, 255));
            }
            p.setStyle(Paint.Style.FILL);
            c.drawRect(0.0f, 0.0f, offsetX * 2, offsetY * 2, p);
            if (que > 2) {
                framec = 0;
                isStartGame = true;
            }
        }

        public void showResult(Canvas c, int margin, int initTime, int currentTime) {
            if (currentTime > initTime + margin) {
                showButton(c);

                int blue = Color.rgb(0x02, 0xff, 0xc5), red = Color.RED;
                int drawColor;
                for (int i = 0; i < qTotal; i++) {
                    Path answerPath = new Path();
                    float hexaRadius = offsetX / 8;
                    float hexaMargin = 10;
                    float totalMargin = hexaMargin * (qTotal - 1);
                    float height = (qTotal - 1) * (offsetX / 5);
                    float x = offsetX / 6;
                    float y = offsetY / 2 - (height / 2 + totalMargin) + i * (hexaRadius + hexaMargin) * 2;
                    PointF giveOrigin = new PointF(x, y);
                    if (judgeLocus(answerThroughList[i], throughList[i])) {
                        drawColor = blue;
                    } else {
                        drawColor = red;
                    }
                    p.setColor(Color.argb(80, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor)));
                    p.setStyle(Paint.Style.FILL);
                    c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
                    p.setColor(Color.argb(255, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor)));
                    p.setStyle(Paint.Style.STROKE);
                    c.drawPath(makeHexagon(giveOrigin, hexaRadius), p);
                    for (int j = 0; j < answerThroughList[i].dots.size(); j++) {
                        if (j == 0) {
                            answerPath.moveTo(x - hexaRadius + dots[answerThroughList[i].dots.get(j)].x / 8, y + (float)(dots[answerThroughList[i].dots.get(j)].y - offsetY * 1.2) / 8);
                        } else {
                            answerPath.lineTo(x - hexaRadius + dots[answerThroughList[i].dots.get(j)].x / 8, y + (float)(dots[answerThroughList[i].dots.get(j)].y - offsetY * 1.2) / 8);
                        }
                    }
                    p.setStrokeWidth(3);
                    c.drawPath(answerPath, p);
                    p.setStyle(Paint.Style.FILL);
                    p.setStrokeWidth(1);
                    p.setTextSize(70);
                    p.setTextAlign(Paint.Align.LEFT);
                    c.drawText(correctStr.get(i), x * 2, giveOrigin.y + 25, p);
                }
            }
        }

        public boolean judgeLocus(ThroughList answer, ThroughList through) {
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
                    for (int[] path: passedPaths) {
                        int[] tempPaths = {path[1], path[0]};
                        if (Arrays.equals(answerPaths.get(i), path) || Arrays.equals(answerPaths.get(i), tempPaths)) {
                            clearFrags[i] = true;
                        }
                    }
                }
                int clearC = 0;
                for (boolean flag : clearFrags) {
                    if (flag) {
                        clearC++;
                    }
                }
                return (clearC == answerPaths.size());
            }
        }

        public void setLocusStart(float x, float y, boolean doCD) {
            if (isStartGame && !isReleased) {
                for (int i = 0; i < 3; i++) {
                    int blurR = (int) (Math.random() * offsetX * 0.8 / 18);
                    double blurA = Math.random() * Math.PI * 2;

                    Point locus = new Point((int) x + (int) (blurR * Math.cos(blurA)), (int) y + (int) (blurR * Math.sin(blurA)));
                    Locus.add(locus);
                }
            } else {
                Locus.add(new Point((int) x, (int) y));
            }

            if (doCD) {
                isCollision(x, y, x, y);
            }
            locusPath.moveTo(x, y);
        }

        public void setLocus(float x, float y, boolean doCD) {
            if (isStartGame && !isReleased) {
                for (int i = 0; i < 3; i++) {
                    int blurR = (int) (Math.random() * offsetX * 0.8 / 18);
                    double blurA = Math.random() * Math.PI * 2;

                    Point locus = new Point((int) x + (int) (blurR * Math.cos(blurA)), (int) y + (int) (blurR * Math.sin(blurA)));
                    Locus.add(locus);
                }
            } else {
                Locus.add(new Point((int) x, (int) y));
            }

            if (doCD) {
                isCollision(x, y, Locus.get(Locus.size() - 2).x, Locus.get(Locus.size() - 2).y);
            }
            locusPath.lineTo(x, y);
        }

        public void isCollision(float x0, float y0, float x1, float y1) {
            int collisionDot = -1;
            for (int i = 0; i < 11; i++) {
                if (x0 == x1 && y0 == y1) {
                    //円の方程式にて当たり判定
                    if ((x0 - dots[i].x) * (x0 - dots[i].x) + (y0 - dots[i].y) * (y0 - dots[i].y) < (offsetX * 0.8 / 18 + 20) * (offsetX * 0.8 / 18 + 20) && state) {
                        isThrough[i] = true;
                        collisionDot = i;
                    }
                }
                //線分と円の当たり判定
                float a = y0 - y1, b = x1 - x0, c = x0 * y1 - x1 * y0;
                double d = (a * dots[i].x + b * dots[i].y + c) / Math.sqrt(a * a + b * b);
                double lim = offsetX * 0.8 / 18 + 20;
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
        //float lim = 40;
        boolean isReleased = false;

        public boolean onTouchEvent(MotionEvent event) {
            float upX, upY;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: //タッチ
                    downX = event.getX();
                    downY = event.getY();
                    if (isStartGame && !isEndGame) {
                        if (isReleased) {
                            resetLocus();
                            resetThrough();
                            isReleased = false;
                        }
                        setLocusStart(downX, downY, true);
                        memX = downX;
                        memY = downY;
                    }
                    break;
                case MotionEvent.ACTION_MOVE: //スワイプ
                    float currentX = event.getX();
                    float currentY = event.getY();
                    if (isStartGame && !isEndGame) {
                        //if (currentX + lim < memX || memX + lim < currentX || currentY + lim < memY || memY + lim < currentY) {
                        if (Locus.size() == 0) {
                            setLocusStart(currentX, currentY, true);
                        }
                        setLocus(currentX, currentY, true);
                        memX = currentX;
                        memY = currentY;
                        //}
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: //リリース
                    upX = event.getX();
                    upY = event.getY();
                    boolean isOnButton;
                    isOnButton = isStartGame &&
                            buttonPoint[0].x <= downX && downX <= buttonPoint[1].x && buttonPoint[0].y <= downY && downY <= buttonPoint[1].y &&
                            buttonPoint[0].x <= upX && upX <= buttonPoint[1].x && buttonPoint[0].y <= upY && upY <= buttonPoint[1].y;

                    if (isStartGame && !isEndGame) {
                        if (!isOnButton) {
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
                                isEndGame = true;
                                for (int i = 0; i < qTotal; i++) {
                                    Log.v("echo", "q[" + i + "]:" + judgeLocus(answerThroughList[i], throughList[i]));
                                }
                            }
                        }
                    }
                    if (isStartGame && isOnButton) {
                        if (doShow) {
                            framec = defTime * 4;
                        } else {
                            startActivity(new Intent(MyActivity.this, MyActivity.class));
                        }
                    }
                    break;
            }
            return true;
        }
    }
}
