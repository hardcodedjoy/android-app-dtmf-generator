/*

MIT License

Copyright © 2023 HARDCODED JOY S.R.L. (https://hardcodedjoy.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

package com.hardcodedjoy.example.dtmfgenerator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.hardcodedjoy.noisoid.Noisoid;
import com.hardcodedjoy.util.GuiUtil;
import com.hardcodedjoy.noisoid.SineGenerator;
import com.hardcodedjoy.util.ThemeUtil;

import java.util.Arrays;

public class MainActivity extends Activity {

    // Button layout and frequencies according to Wikipedia
    // https://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling

    static private final String[] buttonsText = {
            "1", "2", "3", "A",
            "4", "5", "6", "B",
            "7", "8", "9", "C",
            "*", "0", "#", "D"
    };

    static private final int[] freq1 = {
            697, 697, 697, 697,
            770, 770, 770, 770,
            852, 852, 852, 852,
            941, 941, 941, 941
    };

    static private final int[] freq2 = {
            1209, 1336, 1477, 1633,
            1209, 1336, 1477, 1633,
            1209, 1336, 1477, 1633,
            1209, 1336, 1477, 1633
    };

    // Source ids:

    static private final int[] sources1 = new int[16];
    static private final int[] sources2 = new int[16];

    static private final int RQ_CODE_SETTINGS = 1;

    private LinearLayout llMenuOptions;

    static private Noisoid noisoid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings settings = new Settings(getSharedPreferences(
                getPackageName(), Context.MODE_PRIVATE));

        ThemeUtil.setResIdThemeLight(R.style.AppThemeLight);
        ThemeUtil.setResIdThemeDark(R.style.AppThemeDark);
        ThemeUtil.set(this, settings.getTheme());

        initGUI();

        int sampleRate = (int) settings.getSampleRate();

        if(noisoid == null) {
            noisoid = new Noisoid(sampleRate, 10);
            noisoid.start();
            Arrays.fill(sources1, -1);
            Arrays.fill(sources2, -1);
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
    private void initGUI() {

        // we use our own title bar in "layout_main"
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        FrameLayout flMenuOptions = findViewById(R.id.fl_menu_options);
        llMenuOptions = (LinearLayout) flMenuOptions.getChildAt(0);

        findViewById(R.id.iv_menu).setOnClickListener(view -> {
            if(llMenuOptions.getVisibility() == View.VISIBLE) {
                llMenuOptions.setVisibility(View.GONE);
            } else if(llMenuOptions.getVisibility() == View.GONE) {
                llMenuOptions.setVisibility(View.VISIBLE);
            }
        });
        llMenuOptions.setVisibility(View.GONE);
        flMenuOptions.setOnTouchListener((v, event) -> {
            if(llMenuOptions.getVisibility() == View.VISIBLE) {
                llMenuOptions.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        GuiUtil.setOnClickListenerToAllButtons(findViewById(R.id.ll_main), view -> {
            llMenuOptions.setVisibility(View.GONE);
            int id = view.getId();

            if(id == R.id.btn_about) {
                startActivity(new Intent(this, AboutActivity.class));
            }
            if(id == R.id.btn_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, RQ_CODE_SETTINGS);
            }
        });

        LinearLayout llContent = findViewById(R.id.ll_content);
        llContent.removeAllViews();

        LayoutInflater inflater = getLayoutInflater();
        LinearLayout llRow;

        LinearLayout.LayoutParams params;
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0);
        params.weight = 1.0f;

        View.OnTouchListener otl = (v, event) -> {
            int action = event.getAction();
            if(action == MotionEvent.ACTION_DOWN) { onPressed(v); }
            else if(action == MotionEvent.ACTION_UP) { onReleased(v); }
            return false;
        };

        int buttonId = 0;
        for(int i=0; i<4; i++) {
            llRow = (LinearLayout) inflater.inflate(R.layout.layout_4_btn_row, null);
            llRow.setLayoutParams(params);
            int n = llRow.getChildCount();
            for(int j=0; j<n; j++) {
                View view = llRow.getChildAt(j);
                ((Button) view).setText(buttonsText[buttonId]);
                view.setTag(buttonId++);
                view.setOnTouchListener(otl);
            }
            llContent.addView(llRow);
        }
    }

    private void onPressed(View view) {

        float amplitude = 0.4f;

        int sampleRate = noisoid.getSampleRate();
        int id = (int)view.getTag();

        SineGenerator source1 = new SineGenerator(sampleRate, freq1[id]);
        SineGenerator source2 = new SineGenerator(sampleRate, freq2[id]);

        source1.setVolume(amplitude, amplitude);
        source2.setVolume(amplitude, amplitude);

        noisoid.addSource(source1);
        noisoid.addSource(source2);

        sources1[id] = source1.getId();
        sources2[id] = source2.getId();
    }

    private void onReleased(View view) {
        int id = (int)view.getTag();
        noisoid.removeSource(sources1[id]);
        noisoid.removeSource(sources2[id]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) { return; }
        if(requestCode == RQ_CODE_SETTINGS) { recreate(); }
    }

    @Override
    public void onBackPressed() {
        if(llMenuOptions.getVisibility() == View.VISIBLE) {
            llMenuOptions.setVisibility(View.GONE);
            return;
        }

        if(noisoid != null) {
            noisoid.stop();
            noisoid = null;
        }

        super.onBackPressed();
        super.finish();
    }
}