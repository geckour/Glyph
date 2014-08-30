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
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


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
        //int id = item.getItemId();
        //if (id == R.id.action_settings) {
        //    return true;
        //}
        return super.onOptionsItemSelected(item);
    }

    class MyView extends View {
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        float offsetX, offsetY;
        Paint p = new Paint();
        double cr = Math.PI/3;
        double radius;
        PointF[] dots = new PointF[11];

        public MyView(Context context) {
            super(context);

            display.getSize(point);
            offsetX = point.x / 2;
            offsetY = point.y / 2 + (point.y - point.x) / 5;
            radius = offsetX * 0.9;
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
                p.setColor(getResources().getColor(R.color.background));
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(dots[i].x, dots[i].y, dotRadius, p);
            }
        }
    }
}
