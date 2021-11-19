package com.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.R;
import com.common.TransitionsLibrary;
import com.libs.benchmark.BenchmarkLayout;
import com.libs.benchmark.LayoutClassInterface;


public class BenchmarkModeActivity extends AppCompatActivity {
    private LayoutClassInterface benchmarkLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        benchmarkLayout = new BenchmarkLayout(this);
        benchmarkLayout.makeActive();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        TransitionsLibrary.executeToRightTransition(this);
    }
}