package jp.co.example.geckour.glyph;

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
        int qTotal = 0;
        int qNum = 0;

        public class ThroughList {
            ArrayList<Integer> dots;
            public ThroughList(){
                dots = new ArrayList<Integer>();
            }
        }

        public MyView(Context context) {
            super(context);

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

            qTotal = (int)(Math.random() * 4 + 1);
            Log.v("echo", "qTotal:" + qTotal);
            throughList = new ThroughList[qTotal];
            for (int i = 0; i < qTotal; i++) {
                throughList[i] = new ThroughList();
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
                c.drawCircle(point.x, point.y, dotRadius / 2, p);
            }
            p.setColor(Color.RED);
            p.setStyle(Paint.Style.STROKE);
            c.drawPath(locusPath, p);

            p.setTextSize(50);
            p.setColor(Color.WHITE);
            c.drawText(framec / 40 + "." + (framec / 4) % 10, offsetX, offsetY / 6, p);

            framec++;
        }

        public void setLocusStart(float x, float y) {
            isCollision(x, y, x, y);
            locusPath.moveTo(x, y);
            Locus.add(new Point((int)x, (int)y));
        }

        public void setLocus(float x, float y) {
            /*
            for (int i = 0; i < 3; i++){
                int blurR = (int)(Math.random() * offsetX * 0.8 / 15);
                double blurA = Math.random() * Math.PI * 2;

                Point locus = new Point((int)x + (int)(blurR * Math.cos(blurA)), (int)y + (int)(blurR * Math.sin(blurA)));
                Locus.add(locus);
            }
            */
            Locus.add(new Point((int)x, (int)y));

            isCollision(x, y, Locus.get(Locus.size() - 2).x, Locus.get(Locus.size() - 2).y);

            locusPath.lineTo(x, y);
        }

        public void isCollision(float x0, float y0, float x1, float y1) {
            for (int i = 0; i < 11; i++) {
                //円の方程式にて当たり判定
                /*
                if ((x0 - dots[i].x) * (x0 - dots[i].x) + (y0 - dots[i].y) * (y0 - dots[i].y) < (offsetX * 0.8 / 18 + 20) * (offsetX * 0.8 / 18 + 20) && state) {
                    isThrough[i] = true;
                    if(throughList[qNum].dots.size() < 1 || throughList[qNum].dots.get(throughList[qNum].dots.size() - 1) != i) {
                        throughList[qNum].dots.add(i);
                    }
                }
                */
                //線分と円の当たり判定
                float a = y0 - y1, b = x1 - x0, c = x0 * y1 - x1 * y0;
                double d = (a * dots[i].x + b * dots[i].y + c) / Math.sqrt(a * a + b * b);
                double lim = offsetX * 0.8 / 18 + 20;
                if (-lim <= d && d <= lim) {
                    double inner0 = x0 * dots[i].y - dots[i].x * y0, inner1 = x1 * dots[i].y - dots[i].x * y1;
                    double d0 = Math.sqrt((x0 - dots[i].x) * (x0 - dots[i].x) + (y0 - dots[i].y) * (y0 - dots[i].y));
                    double d1 = Math.sqrt((x1 - dots[i].x) * (x1 - dots[i].x) + (y1 - dots[i].y) * (y1 - dots[i].y));
                    if (inner0 * inner1 <= 0) {
                        isThrough[i] = true;
                        if (throughList[qNum].dots.size() < 1 || throughList[qNum].dots.get(throughList[qNum].dots.size() - 1) != i) {
                            throughList[qNum].dots.add(i);
                        }
                    } else if (d0 < lim || d1 < lim) {
                        isThrough[i] = true;
                        if (throughList[qNum].dots.size() < 1 || throughList[qNum].dots.get(throughList[qNum].dots.size() - 1) != i) {
                            throughList[qNum].dots.add(i);
                        }
                    }
                }
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
                    if (isReleased) {
                        resetLocus();
                        resetThrough();
                    }
                    isReleased = false;
                    downX = event.getX();
                    downY = event.getY();
                    memX = downX;
                    memY = downY;
                    setLocusStart(downX, downY);
                    break;
                //スワイプ
                case MotionEvent.ACTION_MOVE:
                    float currentX = event.getX();
                    float currentY = event.getY();
                    if (currentX + lim < memX || memX + lim < currentX || currentY + lim < memY || memY + lim < currentY) {
                        setLocus(currentX, currentY);
                        memX = currentX;
                        memY = currentY;
                    }
                    break;
                //リリース
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isReleased = true;
                    String lists = "";
                    for (int throughDot: throughList[qNum].dots) {
                        lists += "," + throughDot;
                    }
                    Log.v("echo", "lists:" + lists);
                    if (qTotal - 1 > qNum) {
                        qNum++;
                    } else {
                        state = false;
                    }
                    upX = event.getX();
                    upY = event.getY();
                    break;
            }
            return true;
        }
    }
}
