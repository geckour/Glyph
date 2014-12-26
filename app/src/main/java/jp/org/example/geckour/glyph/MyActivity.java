package jp.org.example.geckour.glyph;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Arrays;
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

        Tracker t = ((Analytics) getApplication()).getTracker(Analytics.TrackerName.APP_TRACKER);
        t.setScreenName("MyActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
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
        ArrayList<Point> Locus = new ArrayList<>();
        //ArrayList<Point> blurLocus = new ArrayList<Point>();
        Path locusPath = new Path();
        int framec = 0;
        boolean[] isThrough = new boolean[11];
        ThroughList[] throughList;
        ThroughList[] answerThroughList;
        int qTotal = 0;
        int qNum = 0;
        int defTime = 200;
        int marginTime = 30;
        boolean isEndLoad = false;
        boolean isFirstDraw = true;
        boolean isFirstTimeUp = true;
        boolean isFirstEndGame = true;
        ArrayList<Difficulty> difficulty = new ArrayList<>();
        boolean isStartGame = false;
        boolean isEndGame = false;
        boolean doShow = true;
        Typeface typeface;
        ArrayList<String> correctStr;
        int holdTime;
        Point[] buttonPoint = new Point[2];
        DBHelper dbHelper;
        SQLiteDatabase db;
        Cursor c1, c2;

        public class ThroughList {
            ArrayList<Integer> dots;

            public ThroughList() {
                dots = new ArrayList<>();
            }
            public ThroughList(ArrayList<Integer> argDots) {
                dots = new ArrayList<>(argDots);
            }
            public ThroughList(String[] argDots) {
                dots = new ArrayList<>();
                for (String s: argDots) {
                    try {
                        dots.add(Integer.parseInt(s));
                    } catch (Exception e) {
                        Log.e("", e.getMessage());
                    }
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

        public ArrayList<String> getCorrectStrings(Cursor c) {
            ArrayList<String> strings = new ArrayList<>(Arrays.asList(c.getString(c.getColumnIndex("sequence")).split(",", -1)));
            ArrayList<String> correctStrings = c.isNull(c.getColumnIndex("correctSeq")) ? null : new ArrayList<>(Arrays.asList(c.getString(c.getColumnIndex("correctSeq")).split(",", -1)));

            if (correctStrings != null) {
                ArrayList<String> tStrings = new ArrayList<>();
                for (int i = 0; i < correctStrings.size(); i++) {
                    if (correctStrings.get(i).equals("")) {
                        tStrings.add(strings.get(i));
                    } else {
                        tStrings.add(correctStrings.get(i));
                    }
                }
                return tStrings;
            } else {
                return strings;
            }
        }
        
        public MyView(Context context) {
            super(context);
            dbHelper = new DBHelper(context);
            db = dbHelper.getReadableDatabase();
            c1 = db.query(DBHelper.TABLE_NAME1, null, null, null, null, null, null);
            c2 = db.query(DBHelper.TABLE_NAME2, null, null, null, null, null, null);

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
            qTotal = difficulty.get(level).qs;
            //Log.v("echo", "qTotal:" + qTotal);
            defTime = difficulty.get(level).time;
            if (qTotal > 1) {
                c2.moveToLast();
                long max = c2.getLong(0);
                int randomVal = (int) (Math.random() * max);
                //int randomVal = 0;
                c2.moveToPosition(randomVal);
                while (c2.getInt(c2.getColumnIndex("level")) != qTotal) {
                    randomVal = (int) (Math.random() * max);
                    c2.moveToPosition(randomVal);
                }
                throughList = new ThroughList[qTotal];
                answerThroughList = new ThroughList[qTotal];
                String[] shapesSplit = c2.getString(2).split(",", -1);
                for (int i = 0; i < qTotal; i++) {
                    throughList[i] = new ThroughList();
                    Cursor c = db.rawQuery("select path from " + DBHelper.TABLE_NAME1 + " where name = '" + shapesSplit[i] + "';", null);
                    c.moveToFirst();
                    String[] dotsSplit = c.getString(0).split(",", -1);
                    answerThroughList[i] = new ThroughList(dotsSplit);
                }
                correctStr = getCorrectStrings(c2);
                Log.v("echo", "randomVal:" + randomVal + ", level:" + level);
            } else {
                qTotal = 1;
                //Log.v("echo", "qTotal:" + qTotal);
                c1.moveToLast();
                long max = c1.getLong(0);
                int randomVal = (int) (Math.random() * max);
                throughList = new ThroughList[qTotal];
                answerThroughList = new ThroughList[qTotal];
                throughList[0] = new ThroughList();
                Cursor c = db.rawQuery("select * from " + DBHelper.TABLE_NAME1 + " where id = '" + randomVal + "';", null);
                c.moveToFirst();
                String[] dotsSplit = c.getString(c.getColumnIndex("path")).split(",", -1);
                answerThroughList[0] = new ThroughList(dotsSplit);
                correctStr = new ArrayList<>(Arrays.asList("" + c.getString(c.getColumnIndex("name"))));
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

            isEndLoad = true;

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
            int buttonWidth = doShow ? 250 : 180;
            int buttonHeight = 100;
            int margin = 20;
            buttonPoint[0] = new Point((int)(offsetX * 2 - buttonWidth - margin), (int)(offsetY * 2 - buttonHeight - margin));
            buttonPoint[1] = new Point((int)(offsetX * 2 - margin), (int)(offsetY * 2 - margin));

            p.setColor(getResources().getColor(R.color.button_text));
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(60);
            Drawable drawable = getResources().getDrawable(R.drawable.button);
            drawable.setBounds(buttonPoint[0].x, buttonPoint[0].y, buttonPoint[1].x, buttonPoint[1].y);
            drawable.draw(c);
            if (doShow) {
                c.drawText("BYPASS", buttonPoint[0].x + buttonWidth / 2, buttonPoint[1].y - 30, p);
            } else {
                c.drawText("NEXT", buttonPoint[0].x + buttonWidth / 2, buttonPoint[1].y - 30, p);
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

            if (currentTime - initTime - marginTime >= 0) {
                que = (currentTime - initTime - marginTime) * 2 / (showLength + hideLength);
            }
            if (que < qTotal * 2) {
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
            } else {
                showFlash(c, qTotal * (showLength + hideLength) + marginTime, currentTime, 28);
            }
        }

        public void showFlash(Canvas c, int initTime, int currentTime, int interval) {
            int que;
            int margin = interval / 20;
            int diffTime = currentTime - initTime;
            int alpha = 255;

            que = (diffTime) / interval;
            if (diffTime > interval * 2.5) {
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
            ArrayList<int[]> answerPaths = new ArrayList<>();
            ArrayList<int[]> passedPaths = new ArrayList<>();

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
                    if ((x0 - dots[i].x) * (x0 - dots[i].x) + (y0 - dots[i].y) * (y0 - dots[i].y) < (offsetX * 0.8 / 18 + 30) * (offsetX * 0.8 / 18 + 30) && state) {
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
