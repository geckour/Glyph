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
        ArrayList<ArrayList<String>> shapesSets = new ArrayList<ArrayList<String>>();
        boolean isFirstOnTimeup = true;

        public class ThroughList {
            ArrayList<Integer> dots;
            public ThroughList(){
                dots = new ArrayList<Integer>();
            }
            public ThroughList(ArrayList<Integer> argDot){
                dots = new ArrayList<Integer>(argDot);
            }
        }

        public MyView(Context context) {
            super(context);

            ArrayList<Integer> giveDot;
            int counter = 0;
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 2, 9, 8));
            shapes.put("Abandon", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 1));
            shapes.put("Adapt", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3, 9));
            shapes.put("Advance", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 0, 4, 1));
            shapes.put("Again", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 7, 8, 9, 10, 5));
            shapes.put("All", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1, 0));
            shapes.put("Answer", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 5, 4, 7));
            shapes.put("Attack", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 5, 4, 6, 1));
            shapes.put("Avoid", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 1, 7));
            shapes.put("Barrier", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 2, 8, 1));
            shapes.put("Begin", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 3, 4, 1, 8));
            shapes.put("Being", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 3, 4));
            shapes.put("Body", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 4, 6));
            shapes.put("Breathe", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 1, 0, 2, 9, 8));
            shapes.put("Capture", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 8, 1));
            shapes.put("Change", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 10, 5, 6, 4, 0, 2, 8));
            shapes.put("Chaos", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 8));
            shapes.put("Clear", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 8, 9, 10, 5, 6, 7, 8));
            shapes.put("Clear All", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 0, 2));
            shapes.put("Complex", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 1, 4, 7));
            shapes.put("Conflict", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 1, 7));
            shapes.put("Consequence", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 7, 8, 2, 3, 0, 4));
            shapes.put("Contemplate", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 4, 7));
            shapes.put("Contract", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 1));
            shapes.put("Courage", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 6));
            shapes.put("Create", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 8, 2, 10, 3, 0));
            shapes.put("Creativity", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 9, 10, 3, 0, 1, 7, 6, 4));
            shapes.put("Mind", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3, 0, 8));
            shapes.put("Danger", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 8, 1, 6));
            shapes.put("Defend", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 7, 8));
            shapes.put("Destination", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 4, 1, 2, 8));
            shapes.put("Destiny", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 1, 7));
            shapes.put("Destroy", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 1, 7));
            shapes.put("Die", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1, 4, 6));
            shapes.put("Difficult", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 7, 8, 9));
            shapes.put("Discover", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 10, 9));
            shapes.put("Distance", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 0, 2, 8));
            shapes.put("Easy", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 5, 6, 1, 8));
            shapes.put("End", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 7, 6, 5, 3, 0, 4, 3));
            shapes.put("Enlightened_A", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 8, 7, 6, 5, 3, 0, 4, 3));
            shapes.put("Enlightened_B", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1));
            shapes.put("Equal", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 6, 4, 3, 2));
            shapes.put("Escape", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 3, 2));
            shapes.put("Evolution", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 4, 1));
            shapes.put("Failure", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1, 6));
            shapes.put("Fear", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 6, 7));
            shapes.put("Follow", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 9));
            shapes.put("Forget", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 1, 7));
            shapes.put("Future", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2));
            shapes.put("Gain", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 1, 4, 6));
            shapes.put("Government", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2));
            shapes.put("Grow", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(7, 1, 0, 3, 5, 4, 0));
            shapes.put("Harm", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 1, 8, 2, 0, 4, 5, 3, 0));
            shapes.put("Harmony", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 0, 2, 8));
            shapes.put("Have", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 2, 1));
            shapes.put("Help", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 5, 6, 1, 8));
            shapes.put("End", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 6, 1, 2));
            shapes.put("Hide", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 4, 8));
            shapes.put("I", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(1, 7));
            shapes.put("Ignore", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 0, 2, 3, 0));
            shapes.put("Imperfect", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 1));
            shapes.put("Improve", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 3, 2, 0));
            shapes.put("Impure", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 3, 10, 9, 2, 0, 8));
            shapes.put("Interrupt", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 4, 0, 3, 10, 9, 8));
            shapes.put("Journey", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 0, 4, 8));
            shapes.put("Knowledge", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 10, 9, 2, 8));
            shapes.put("Lead", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 10, 5, 6, 4, 1, 7));
            shapes.put("Legacy", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 4));
            shapes.put("Less", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 0, 4, 6, 5));
            shapes.put("Liberate", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 0, 1, 4, 0));
            shapes.put("Lie", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 2, 0, 4, 6));
            shapes.put("Live Again", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(6, 1));
            shapes.put("Lose", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 0, 1, 6));
            shapes.put("Message", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 3, 0, 8));
            shapes.put("Idea", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1));
            shapes.put("More", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 5, 4, 3, 2));
            shapes.put("Mystery", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 4, 1, 7));
            shapes.put("Nature", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 1, 7));
            shapes.put("New", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 1));
            shapes.put("No", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 9, 2, 0, 8));
            shapes.put("Nourish", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2));
            shapes.put("Old", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 1, 8));
            shapes.put("Open", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 2, 1, 8, 9, 10, 5, 6, 7, 8));
            shapes.put("Open All", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 4, 6, 7, 1, 2, 9, 10));
            shapes.put("Opening", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 9));
            shapes.put("Past", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 2, 9));
            shapes.put("Path", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 2, 9, 8, 7, 1, 0));
            shapes.put("Perfection", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 5, 3, 0, 1, 7));
            shapes.put("Perspective", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 1, 7, 6));
            shapes.put("Potential", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 0, 4, 1, 2, 8, 1));
            shapes.put("Presence", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 2, 1, 4));
            shapes.put("Present", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 0, 4, 1, 0));
            shapes.put("Pure", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 5, 4));
            shapes.put("Pursue", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 3, 2));
            shapes.put("Question", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 0, 1, 7));
            shapes.put("React", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 4, 6, 7));
            shapes.put("Rebel", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 3, 10, 5, 0));
            shapes.put("Recharge", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 5, 0, 8, 2));
            shapes.put("Resistance_A", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(4, 3, 5, 0, 8, 1));
            shapes.put("Resistance_B", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 0, 1, 7, 8));
            shapes.put("Restraint", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 4, 7));
            shapes.put("Retreat", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 3, 4, 7));
            shapes.put("Safety", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 0, 1, 6));
            shapes.put("Save", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 3));
            shapes.put("See", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 4, 3, 2, 1));
            shapes.put("Seek", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 8, 7));
            shapes.put("Self", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 2, 0, 4, 1, 7));
            shapes.put("Separate", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 3, 5, 4, 1, 7));
            shapes.put("Shaper", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(7, 1, 2, 9, 8));
            shapes.put("Share", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 1));
            shapes.put("Simple", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 0, 4, 1, 8));
            shapes.put("Soul", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 1, 7));
            shapes.put("Stability", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1, 2));
            shapes.put("Strong", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 0, 4, 3, 0));
            shapes.put("Together", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 0, 1, 4, 0, 2, 3));
            shapes.put("Truth", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(0, 1, 6));
            shapes.put("Use", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(8, 3, 5, 4, 8));
            shapes.put("Victory", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(9, 2, 8, 1));
            shapes.put("Want", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(3, 4, 8));
            shapes.put("We", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 3, 4, 1));
            shapes.put("Weak", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(10, 2, 0, 1, 6));
            shapes.put("Worth", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 1, 0, 2));
            shapes.put("XM", new ThroughList(giveDot));
            shapes.put("" + counter++, new ThroughList(giveDot));
            giveDot = new ArrayList<Integer>(Arrays.asList(5, 2, 1, 5));
            shapes.put("You", new ThroughList(giveDot));
            shapes.put("" + counter, new ThroughList(giveDot));

            ArrayList<String> giveStrings;
            giveStrings = new ArrayList<String>(Arrays.asList("Past", "Present", "Future"));
            shapesSets.add(giveStrings);

            //qTotal = (int)(Math.random() * 4 + 1);
            qTotal = 3;
            throughList = new ThroughList[qTotal];
            answerThroughList = new ThroughList[qTotal];
            for (int i = 0; i < qTotal; i++) {
                throughList[i] = new ThroughList();
                int randomVal = (int)(Math.random()*shapesSets.size());
                while (shapesSets.get(randomVal).size() != qTotal) {
                    randomVal = (int)(Math.random()*shapesSets.size());
                }
                answerThroughList[i] = shapes.get(shapesSets.get(randomVal).get(i));
                /*
                int randomVal = (int)(Math.random()*counter);
                answerThroughList[i] = shapes.get("" + randomVal);
                */
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

            if (doCount) {
                framec++;
            } else {
                float width = (qTotal - 1) * dotRadius * 6;
                for (int i = 0; i < qTotal; i++) {
                    if (judgeLocus(answerThroughList[i], throughList[i])) {
                        p.setColor(Color.WHITE);
                    } else {
                        p.setColor(Color.BLACK);
                    }
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(offsetX - width / 2 + i * dotRadius * 6, offsetY / 4, dotRadius * 3 / 2, p);
                }
            }
        }

        public void showAnswer(Canvas c) {

        }

        public boolean judgeLocus (ThroughList answer, ThroughList throughed) {
            ArrayList<int[]> answerPaths = new ArrayList<int[]>();
            ArrayList<int[]> passedPaths = new ArrayList<int[]>();

            if (answer.dots.size() != throughed.dots.size()) {
                return false;
            } else {
                for (int i = 0; i < answer.dots.size() - 1; i++) {
                    int[] path0 = {answer.dots.get(i), answer.dots.get(i + 1)};
                    answerPaths.add(path0);
                    int[] path1 = {throughed.dots.get(i), throughed.dots.get(i + 1)};
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
                for (int i = 0; i < clearFrags.length; i++) {
                    if (clearFrags[i]) {
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
