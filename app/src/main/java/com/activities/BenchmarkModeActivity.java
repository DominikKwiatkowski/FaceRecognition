package com.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.libs.benchmark.BenchmarkLayout;
import com.libs.benchmark.LayoutClassInterface;


public class BenchmarkModeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutClassInterface benchmarkLayout = new BenchmarkLayout(this);
        benchmarkLayout.makeActive();
    }
}