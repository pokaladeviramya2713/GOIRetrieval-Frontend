package com.simats.goiretrieval

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val btnNext = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_next)
        val tvSkip = findViewById<android.widget.TextView>(R.id.tv_skip)
        
        val highlighter = findViewById<android.view.View>(R.id.indicator_highlighter)
        val dot1 = findViewById<android.view.View>(R.id.dot_1)
        val dot2 = findViewById<android.view.View>(R.id.dot_2)
        val dot3 = findViewById<android.view.View>(R.id.dot_3)
        val dots = listOf(dot1, dot2, dot3)

        val pages = listOf(
            OnboardingPage("Smart Search", "Instantly search across government\nregulations, policies, schemes, and circulars\nwith intelligent keyword matching.", R.drawable.ic_search_navy),
            OnboardingPage("AI-Powered Insights", "Get intelligent analysis and relevant data\nextracted from authentic government\nsources for informed decisions.", R.drawable.ic_star_navy),
            OnboardingPage("Quick Decisions", "Access the right information instantly for\nefficient coordination and faster\nacross departments.", R.drawable.ic_bolt_navy)
        )

        val adapter = OnboardingAdapter(pages)
        viewPager.adapter = adapter

        // Anti-Gravity Momentum Indicator Logic:
        // Distance between dots: 24dp. Base width 32dp.
        val dotDistance = 24 * resources.displayMetrics.density
        val baseWidth = 32 * resources.displayMetrics.density
        
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                
                // Momentum Elongation:
                // Dash expands significantly (up to 2x dotDistance) during movement
                val expansionVelocity = if (positionOffset < 0.5f) {
                    positionOffset * 2.0f
                } else {
                    (1.0f - positionOffset) * 2.0f
                }
                
                // Increase stretch for "Anti-Gravity/Momentum" feel
                val currentWidth = baseWidth + (expansionVelocity * dotDistance * 1.5f)
                
                val layoutParams = highlighter.layoutParams
                layoutParams.width = currentWidth.toInt()
                highlighter.layoutParams = layoutParams

                // Center-based translation
                val dot1Center = 4 * resources.displayMetrics.density
                val targetCenter = dot1Center + (position + positionOffset) * dotDistance
                highlighter.translationX = targetCenter - (currentWidth / 2f)

                // Alpha fade for background dots
                for (i in dots.indices) {
                    val distance = Math.abs(position + positionOffset - i)
                    dots[i].alpha = Math.max(0.1f, Math.min(1.0f, distance * 2.0f))
                }
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                btnNext.text = if (position == 2) "Get Started" else "Next"
            }
        })

        // Backward Flow (Back Logic):
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem > 0) {
                    // Smooth reverse slide to previous screen
                    viewPager.setCurrentItem(viewPager.currentItem - 1, true)
                } else {
                    // Screen 1 exit: Subtle scale-down and fade-out
                    window.decorView.animate()
                        .alpha(0f)
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(300)
                        .withEndAction { 
                            finish()
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, android.R.anim.fade_out)
                            } else {
                                @Suppress("DEPRECATION")
                                overridePendingTransition(0, android.R.anim.fade_out)
                            }
                        }
                        .start()
                }
            }
        })

        // Button Feedback & Navigation
        btnNext.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).setInterpolator(android.view.animation.OvershootInterpolator()).start()
                }
            }
            false
        }

        btnNext.setOnClickListener {
            if (viewPager.currentItem < 2) {
                viewPager.setCurrentItem(viewPager.currentItem + 1, true)
            } else {
                navigateToMain()
            }
        }

        tvSkip.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = android.content.Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

    data class OnboardingPage(val title: String, val subtitle: String, val iconRes: Int)

    class OnboardingAdapter(private val pages: List<OnboardingPage>) : 
        androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {
        
        class PageViewHolder(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val ivIcon: android.widget.ImageView = view.findViewById(R.id.iv_icon)
            val tvTitle: android.widget.TextView = view.findViewById(R.id.tv_title)
            val tvSubtitle: android.widget.TextView = view.findViewById(R.id.tv_subtitle)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PageViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val page = pages[position]
            holder.ivIcon.setImageResource(page.iconRes)
            holder.tvTitle.text = page.title
            holder.tvSubtitle.text = page.subtitle
        }

        override fun getItemCount(): Int = pages.size
    }
}
