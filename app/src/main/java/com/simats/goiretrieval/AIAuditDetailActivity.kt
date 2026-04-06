package com.simats.goiretrieval

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.simats.goiretrieval.api.AuditDocument
import com.simats.goiretrieval.utils.AuditPdfGenerator

class AIAuditDetailActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabSummary: TextView
    private lateinit var tabOriginal: TextView
    private lateinit var tabAnalysis: TextView
    private lateinit var tabIndicator: View
    private lateinit var tvTitle: TextView
    private var doc: AuditDocument? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_audit_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        @Suppress("DEPRECATION")
        doc = intent.getSerializableExtra("document") as? AuditDocument




        
        initializeViews()
        setupViewPager()
        setupListeners()
        updateUI()
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.view_pager)
        tabSummary = findViewById(R.id.tab_summary)
        tabOriginal = findViewById(R.id.tab_original)
        tabAnalysis = findViewById(R.id.tab_analysis)
        tabIndicator = findViewById(R.id.tab_indicator)
        tvTitle = findViewById(R.id.tv_detail_title)
    }

    private fun setupViewPager() {
        doc?.let {
            val adapter = AuditDetailPagerAdapter(it)
            viewPager.adapter = adapter
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val totalWidth = (tabSummary.parent as View).width
                val tabWidth = totalWidth / 3
                val params = tabIndicator.layoutParams as ViewGroup.MarginLayoutParams
                params.leftMargin = ((position + positionOffset) * tabWidth).toInt()
                tabIndicator.layoutParams = params
            }

            override fun onPageSelected(position: Int) {
                updateTabs(position)
            }
        })
    }

    private fun updateTabs(position: Int) {
        val navy = ContextCompat.getColor(this, R.color.navy_home)
        val grey = ContextCompat.getColor(this, R.color.grey_text)

        tabSummary.setTextColor(if (position == 0) navy else grey)
        tabSummary.typeface = if (position == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        
        tabOriginal.setTextColor(if (position == 1) navy else grey)
        tabOriginal.typeface = if (position == 1) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        
        tabAnalysis.setTextColor(if (position == 2) navy else grey)
        tabAnalysis.typeface = if (position == 2) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }

        // Download removed from this screen as per user request. 
        // Reports can be downloaded from the 'Reports' section on the Home Page.

        tabSummary.setOnClickListener { viewPager.currentItem = 0 }
        tabOriginal.setOnClickListener { viewPager.currentItem = 1 }
        tabAnalysis.setOnClickListener { viewPager.currentItem = 2 }
    }

    private fun updateUI() {
        tvTitle.text = doc?.title ?: "Audit Document Detail"
    }
}
