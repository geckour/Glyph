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
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
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
    int countView = 0;
    MyView view;
    float offsetX;
    float offsetY;
    boolean isFocused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getInt("countView", -1) != -1) {
            countView = sp.getInt("countView", 0);
        } else {
            countView = 1;
        }
        sp.edit().putInt("countView", countView + 1).apply();

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
        ArrayList<Particle> Locus = new ArrayList<>();
        Path locusPath = new Path();
        int framec = 0;
        boolean[] isThrough = new boolean[11];
        ThroughList[] throughList;
        ThroughList[] answerThroughList;
        int qTotal = 0;
        int qNum = 0;
        int defTime = 20000;
        long initTime;
        long marginTime = 1200;
        boolean isEndLoad = false;
        boolean isFirstDraw = true;
        boolean isFirstTimeUp = true;
        boolean isFirstEndGame = true;
        boolean doVibrate = false;
        boolean showCountView = false;
        ArrayList<Difficulty> difficulty = new ArrayList<>();
        boolean isStartGame = false;
        boolean isEndGame = false;
        boolean doShow = true;
        Typeface typeface;
        ArrayList<String> correctStr;
        long holdTime;
        Point[] buttonPoint = new Point[2];
        DBHelper dbHelper;
        SQLiteDatabase db;
        Cursor c1, c2;
        int previousDot = -1;
        Canvas c;

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

            int giveTime = 20000;
            int giveQs = 1;
            for (int i = 0; i < 9; i++) {
                if (i > 3) {
                    giveTime -= 1000;
                }
                if (i == 2 || i == 3 || i == 6 || i == 8) {
                    giveQs++;
                }
                difficulty.add(i, new Difficulty(giveQs, giveTime));
            }
            gameMode = Integer.parseInt(sp.getString("gamemode", "0"));
            doVibrate = sp.getBoolean("doVibrate", false);
            showCountView = sp.getBoolean("showCountView", false);
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
                    Log.v("echo", "path: " + c.getString(0));
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
            long now = System.currentTimeMillis();
            this.c = c;
            c.drawColor(getResources().getColor(R.color.background));

            if (isFirstDraw) {
                initTime = System.currentTimeMillis();
                radius = offsetX * 0.8;
                dots[0] = new PointF(offsetX, (float) (offsetY * 1.2));
                for (int i = 1; i < 5; i++) {
                    int j = i;
                    if (i > 1) {
                        j++;
                        if (i > 3) {
                            j++;
                        }
                    }
                    dots[i] = new PointF((float) (Math.cos(cr * (j - 0.5)) * (radius / 2) + offsetX), (float) (Math.sin(cr * (j - 0.5)) * (radius / 2) + offsetY * 1.2));
                }
                for (int i = 5; i < 11; i++) {
                    dots[i] = new PointF((float) (Math.cos(cr * (i - 0.5)) * radius + offsetX), (float) (Math.sin(cr * (i - 0.5)) * radius + offsetY * 1.2));
                }

                isFirstDraw = false;
            }

            if (isEndLoad) {
                if (showCountView) {
                    showCount();
                }

                if (!isStartGame) {
                    showAnswer(initTime, now);
                }
                if (isEndGame) {
                    if (isFirstEndGame) {
                        holdTime = now;
                        isFirstEndGame = false;
                    }
                    if (System.currentTimeMillis() > holdTime + marginTime) {
                        doShow = false;
                        showResult(marginTime, holdTime + marginTime, now);
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
                    for (Particle particle : Locus) {
                        particle.move();
                    }
                    if (!isStartGame || isReleased) {
                        p.setColor(Color.WHITE);
                        p.setStrokeWidth(3);
                        p.setStyle(Paint.Style.STROKE);
                        c.drawPath(locusPath, p);
                        p.setStrokeWidth(0);
                    }
                    showButton();
                }

                if (isStartGame && doShow) {
                    showTime(System.currentTimeMillis());
                    showQueNumber(framec, 0, Color.argb(50, 0x02, 0xff, 0xc5), Color.argb(100, 0x02, 0xff, 0xc5));
                } else if (!isStartGame) {
                    showQueNumber(now, marginTime, Color.argb(50, 220, 175, 50), Color.argb(100, 220, 175, 50));
                }

                framec++;
            }
        }

        class Particle {
            float x0, y0;
            Grain grain[] = new Grain[3];
            int phase = 0;
            long moveFrames = 400;
            long initFrames = 0;
            float grainR = 10;
            double v = 0.15;

            public Particle(float x0, float y0) {
                this.x0 = x0;
                this.y0 = y0;
                initFrames = System.currentTimeMillis();
                grain[0] = new Grain(x0, y0);
                grain[1] = new Grain(x0, y0);
                grain[2] = new Grain(x0, y0);
            }

            public void move() {
                long diffFrames = System.currentTimeMillis() - initFrames;
                if (diffFrames > moveFrames || isReleased || !isStartGame) phase = 1;

                if (phase == 0) {
                    grain[0].x = grain[0].step0[0] + grain[0].diff[0] * (moveFrames - diffFrames) / moveFrames;
                    grain[0].y = grain[0].step0[1] + grain[0].diff[1] * (moveFrames - diffFrames) / moveFrames;
                    grain[1].x = grain[1].step0[0] + grain[1].diff[0] * (moveFrames - diffFrames) / moveFrames;
                    grain[1].y = grain[1].step0[1] + grain[1].diff[1] * (moveFrames - diffFrames) / moveFrames;
                    grain[2].x = grain[2].step0[0] + grain[2].diff[0] * (moveFrames - diffFrames) / moveFrames;
                    grain[2].y = grain[2].step0[1] + grain[2].diff[1] * (moveFrames - diffFrames) / moveFrames;
                }
                if (phase == 1) {
                    grain[0].x += (float) (Math.cos(grain[0].a1) * grain[0].circleR * Math.cos(grain[0].a0));
                    grain[0].y += (float) (Math.sin(grain[0].a1) * grain[0].circleR * Math.cos(grain[0].a0));
                    grain[0].a0 += v;
                    grain[1].x += (float) (Math.cos(grain[1].a1) * grain[1].circleR * Math.cos(grain[1].a0));
                    grain[1].y += (float) (Math.sin(grain[1].a1) * grain[1].circleR * Math.cos(grain[1].a0));
                    grain[1].a0 += v;
                    grain[2].x += (float) (Math.cos(grain[2].a1) * grain[2].circleR * Math.cos(grain[2].a0));
                    grain[2].y += (float) (Math.sin(grain[2].a1) * grain[2].circleR * Math.cos(grain[2].a0));
                    grain[2].a0 += v;
                }
                draw();
            }

            private void draw() {
                for (Grain gr : grain) {
                    p.setShader(new RadialGradient(gr.x, gr.y, grainR, Color.WHITE, Color.argb(0, 220, 175, 50), Shader.TileMode.CLAMP));
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(gr.x, gr.y, grainR, p);
                }
                p.setShader(null);
            }
        }

        class Grain {
            float x, y;
            float origin[] = new float[2];
            float step0[] = new float[2];
            float step1[] = new float[2];
            float diff[] = new float[2];
            double a0 = Math.random() * Math.PI * 2;
            double a1 = Math.random() * Math.PI * 2;
            double circleR = Math.random() * 0.5 + 0.7;

            public Grain(float x, float y) {
                origin[0] = x;
                origin[1] = y;

                double blurR = Math.random() * offsetX * 0.05;
                double blurA = Math.random() * Math.PI * 2.0;
                step0[0] = origin[0] + (float) (blurR * Math.cos(blurA));
                step0[1] = origin[1] + (float) (blurR * Math.sin(blurA));


                blurR = offsetX * 0.2 + Math.random() * offsetX * 0.05;
                blurA = Math.random() * Math.PI * 2.0;
                step1[0] = origin[0] + (float) (blurR * Math.cos(blurA));
                step1[1] = origin[1] + (float) (blurR * Math.sin(blurA));

                diff[0] = step1[0] - step0[0];
                diff[1] = step1[1] - step0[1];

                if (isReleased || !isStartGame) {
                    this.x = step0[0];
                    this.y = step0[1];
                } else {
                    this.x = step1[0];
                    this.y = step1[1];
                }
            }
        }

        public void putParticles(ThroughList throughList) {
            float length[] = new float[throughList.dots.size() - 1];
            for (int i = 1; i < throughList.dots.size(); i++) {
                PointF point1 = dots[throughList.dots.get(i)];
                PointF point0 = dots[throughList.dots.get(i - 1)];
                length[i - 1] = (float) Math.sqrt((point1.x - point0.x) * (point1.x - point0.x) + (point1.y - point0.y) * (point1.y - point0.y));
            }
            Locus.clear();
            for (int i = 0; i < length.length; i++) {
                float unitV[] = {(dots[throughList.dots.get(i + 1)].x - dots[throughList.dots.get(i)].x) / length[i], (dots[throughList.dots.get(i + 1)].y - dots[throughList.dots.get(i)].y) / length[i]};
                float x = dots[throughList.dots.get(i)].x;
                float y = dots[throughList.dots.get(i)].y;

                float sumLength[] = {0, 0};
                while (sumLength[0] <= Math.abs(dots[throughList.dots.get(i + 1)].x - dots[throughList.dots.get(i)].x) && sumLength[1] <= Math.abs(dots[throughList.dots.get(i + 1)].y - dots[throughList.dots.get(i)].y)) {
                    Locus.add(new Particle(x, y));
                    x += unitV[0] * 30;
                    y += unitV[1] * 30;
                    sumLength[0] += Math.abs(dots[throughList.dots.get(i + 1)].x - dots[throughList.dots.get(i)].x) * 30 / length[i];
                    sumLength[1] += Math.abs(dots[throughList.dots.get(i + 1)].y - dots[throughList.dots.get(i)].y) * 30 / length[i];
                }
            }
        }

        public void showButton() {
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

        public void showCount() {
            p.setColor(getResources().getColor(R.color.button_text));
            p.setTextSize(40);
            p.setTypeface(typeface);
            p.setTextAlign(Paint.Align.RIGHT);
            float x = (float)(offsetX * 2.0 - 20.0);
            float y = (float)(offsetY * 2.0 - 130.0);

            c.drawText("Hack:" + countView, x, y, p);
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

        public void showTime(long currentTime) {
            p.setColor(Color.rgb(220, 190, 50));
            long leftTime = (defTime - ((isEndGame ? holdTime : currentTime) - initTime)) / 100;
            if (leftTime <= 0) {
                isEndGame = true;
                if (isFirstTimeUp) {
                    for (int i = 0; i < qTotal; i++) {
                        Log.v("echo", "q[" + i + "]:" + judgeLocus(answerThroughList[i], throughList[i]));
                    }
                    isFirstTimeUp = false;
                }
            }
            p.setTextSize(60);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(String.format("%02d", leftTime / 10) + ":" + leftTime % 10, offsetX, offsetY / 3, p);
            float barWidth = (float) (offsetX * 0.7 / defTime) * leftTime * 100;
            p.setStyle(Paint.Style.FILL);
            c.drawRect(offsetX - barWidth, (float)(offsetY / 2.7), offsetX + barWidth, (float)(offsetY / 2.55), p);
        }

        public void showQueNumber(long currentTime, long marginTime, int normalColor, int strongColor) {
            float hexaRadius = offsetX / 10;
            float hexaMargin = 5;
            float totalMargin = hexaMargin * (qTotal - 1);
            float width = (qTotal - 1) * (offsetX / 5);
            float x, y;
            for (int i = 0; i < qTotal; i++) {
                x = offsetX - (width / 2 + totalMargin) + i * (hexaRadius + hexaMargin) * 2;
                y = (float)(offsetY / 7.5);
                PointF origin = new PointF(x, y);

                if (isStartGame) {
                    if (i == qNum) {
                        if ((isReleased && throughList[qTotal - 1].dots.size() > 0)) {
                            p.setColor(normalColor);
                            p.setStyle(Paint.Style.FILL);
                            c.drawPath(makeHexagon(origin, hexaRadius), p);
                        } else {
                            p.setColor(strongColor);
                            p.setStyle(Paint.Style.FILL);
                            c.drawPath(makeHexagon(origin, hexaRadius), p);
                        }
                    } else if (i < qNum) {
                        p.setColor(normalColor);
                        p.setStyle(Paint.Style.FILL);
                        c.drawPath(makeHexagon(origin, hexaRadius), p);
                    } else {
                        p.setColor(Color.BLACK);
                        p.setStyle(Paint.Style.FILL);
                        c.drawPath(makeHexagon(origin, hexaRadius), p);
                    }
                } else {
                    if (i == (currentTime - marginTime) / 50 && currentTime > marginTime) {
                        p.setColor(strongColor);
                        p.setStyle(Paint.Style.FILL);
                        c.drawPath(makeHexagon(origin, hexaRadius), p);
                    } else {
                        p.setColor(Color.BLACK);
                        p.setStyle(Paint.Style.FILL);
                        c.drawPath(makeHexagon(origin, hexaRadius), p);
                    }
                }
                p.setStrokeJoin(Paint.Join.BEVEL);
                p.setColor(Color.argb(255, Color.red(normalColor), Color.green(normalColor), Color.blue(normalColor)));
                p.setStyle(Paint.Style.STROKE);
                c.drawPath(makeHexagon(origin, hexaRadius), p);
            }
        }

        int preQue = -1;
        long initFlashTime = 0;
        boolean isFirstFlash = true;
        public void showAnswer(long initTime, long currentTime) {
            int showLength = 1960;
            int hideLength = 40;
            int que = -1;
            long diffTIme = currentTime - initTime - marginTime;

            if (diffTIme >= 0) {
                que = (int) (diffTIme) * 2 / (showLength + hideLength);
            }
            if (que < qTotal * 2) {
                if (que % 2 == 0 && que >= 0) {
                    for (int i = 0; i < answerThroughList[que / 2].dots.size(); i++) {
                        if (gameMode == 0 || gameMode == 1) {
                            p.setColor(Color.WHITE);
                            p.setTextSize(80);
                            p.setTypeface(typeface);
                            p.setTextAlign(Paint.Align.CENTER);
                            c.drawText(correctStr.get(que / 2), offsetX, offsetY / 3, p);
                        }
                    }
                    if (preQue != que && (gameMode == 0 || gameMode == 2)) {
                        putParticles(answerThroughList[que / 2]);
                    }
                } else {
                    resetLocus();
                }
            } else {
                if (isFirstFlash) {
                    initFlashTime = System.currentTimeMillis();
                    isFirstFlash = false;
                }
                showFlash(initFlashTime, currentTime);
            }
            preQue = que;
        }

        public void showFlash(long initTime, long currentTime) {
            int que;
            int interval = 1000;
            int margin = interval / 800;
            int diffTime = (int) (currentTime - initTime);
            int alpha = 255;

            que = diffTime / interval;
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
                this.initTime = System.currentTimeMillis();
                isStartGame = true;
            }
        }

        public void showResult(long margin, long initTime, long currentTime) {
            if (currentTime > initTime + margin) {
                showButton();

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
            Locus.add(new Particle(x, y));

            if (doCD) {
                isCollision(x, y, x, y);
            }
            locusPath.moveTo(x, y);
        }

        public void setLocus(float x, float y, boolean doCD) {
            Locus.add(new Particle(x, y));

            if (doCD) {
                isCollision(x, y, Locus.get(Locus.size() - 2).x0, Locus.get(Locus.size() - 2).y0);
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
                if (doVibrate) {
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    vibrator.vibrate(50);
                }
                previousDot = collisionDot;
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
        float lim = 20;
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
                        if (currentX + lim < memX || memX + lim < currentX || currentY + lim < memY || memY + lim < currentY) {
                            if (Locus.size() == 0) {
                                setLocusStart(currentX, currentY, true);
                            }
                            setLocus(currentX, currentY, true);
                            memX = currentX;
                            memY = currentY;
                        }
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
                            if (throughList[qNum].dots.size() > 0) {
                                putParticles(throughList[qNum]);
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
