package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.widget.Toast

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuRows(view)
        setupNavigation(view)
        loadUserProfile(view)
        setupNotifications(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadUserProfile(it) }
        updateNotificationDot()
    }

    private fun setupNotifications(view: View) {
        val bell = view.findViewById<View>(R.id.rl_notification_bell) ?: return
        val dot = view.findViewById<View>(R.id.view_notification_dot) ?: return

        bell.setOnClickListener {
            dot.visibility = View.GONE
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
    }

    private fun updateNotificationDot() {
        val view = view ?: return
        val dot = view.findViewById<View>(R.id.view_notification_dot) ?: return
        val userId = SessionManager.getInstance(requireContext()).getUserId()
        
        if (userId == -1) return

        com.simats.goiretrieval.api.RetrofitClient.instance.getNotifications(userId).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.NotificationsResponse> {
            override fun onResponse(
                call: retrofit2.Call<com.simats.goiretrieval.api.NotificationsResponse>,
                response: retrofit2.Response<com.simats.goiretrieval.api.NotificationsResponse>
            ) {
                if (response.isSuccessful && isAdded) {
                    val notifications = response.body()?.notifications ?: emptyList()
                    val hasUnread = notifications.any { !it.is_read }
                    dot.visibility = if (hasUnread) View.VISIBLE else View.GONE
                }
            }
            override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.NotificationsResponse>, t: Throwable) {}
        })
    }

    private fun loadUserProfile(view: View) {
        val tvName = view.findViewById<TextView>(R.id.tv_name)
        val tvAvatar = view.findViewById<TextView>(R.id.tv_avatar)
        
        val name = SessionManager.getInstance(requireContext()).getUserName() ?: "User"
        tvName.text = name
        
        tvAvatar.text = if (name.isNotEmpty()) name[0].uppercaseChar().toString() else "U"
    }

    private fun setupMenuRows(view: View) {
        setupRow(view, R.id.menu_personal_info, "Personal Information", R.drawable.ic_user_outline)
        setupRow(view, R.id.menu_role_permissions, "Role & Permissions", R.drawable.ic_shield_outline)
        setupRow(view, R.id.menu_preferences, "Preferences", R.drawable.ic_gear_outline)
        setupRow(view, R.id.menu_help_support, "Help & Support", R.drawable.ic_question_outline)
        setupRow(view, R.id.menu_security_settings, "Security Settings", R.drawable.ic_shield_check_outline)
        setupRow(view, R.id.menu_privacy_settings, "Privacy Settings", R.drawable.ic_padlock_outline)
        setupRow(view, R.id.menu_about_app, "About Application", R.drawable.ic_info_circle_outline)
    }

    private fun setupRow(parent: View, rowId: Int, title: String, iconResId: Int) {
        val rowView = parent.findViewById<View>(rowId)
        val tvTitle = rowView.findViewById<TextView>(R.id.row_title)
        val ivIcon = rowView.findViewById<ImageView>(R.id.row_icon)
        
        tvTitle.text = title
        ivIcon.setImageResource(iconResId)
    }

    private fun setupNavigation(view: View) {
        view.findViewById<View>(R.id.btn_back_profile)?.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        view.findViewById<View>(R.id.menu_personal_info).setOnClickListener {
            startActivity(Intent(requireContext(), PersonalInfoActivity::class.java))
        }
        view.findViewById<View>(R.id.menu_role_permissions).setOnClickListener {
            startActivity(Intent(requireContext(), RolePermissionsActivity::class.java))
        }
        view.findViewById<View>(R.id.menu_preferences).setOnClickListener {
            startActivity(Intent(requireContext(), PreferencesActivity::class.java))
        }
        
        // Notification Toggle Logic
        val notifSwitch = view.findViewById<SwitchCompat>(R.id.switch_notifications)
        val session = SessionManager.getInstance(requireContext())
        val currentPrefs = session.getNotificationPrefs()
        notifSwitch.isChecked = currentPrefs.first // Using Push as master switch
        
        // Dynamic Color Logic for Toggle
        val navyColor = ContextCompat.getColor(requireContext(), R.color.navy_bg)
        val greyColor = Color.parseColor("#D0D0D0")
        val trackNavy = Color.parseColor("#9EA1D4") // Lighter version for track
        val trackGrey = Color.parseColor("#E0E0E0")

        val thumbStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(navyColor, greyColor)
        )
        val trackStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(trackNavy, trackGrey)
        )

        notifSwitch.thumbTintList = thumbStates
        notifSwitch.trackTintList = trackStates
        
        notifSwitch.setOnCheckedChangeListener { _, isChecked ->
            session.saveNotificationPrefs(isChecked, currentPrefs.second, currentPrefs.third)
            val msg = if (isChecked) "Notifications Enabled" else "Notifications Disabled"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menu_help_support).setOnClickListener {
            startActivity(Intent(requireContext(), HelpSupportActivity::class.java))
        }
        view.findViewById<View>(R.id.menu_security_settings).setOnClickListener {
            startActivity(Intent(requireContext(), SecuritySettingsActivity::class.java))
        }
        view.findViewById<View>(R.id.menu_privacy_settings).setOnClickListener {
            startActivity(Intent(requireContext(), PrivacySettingsActivity::class.java))
        }
        view.findViewById<View>(R.id.menu_about_app).setOnClickListener {
            startActivity(Intent(requireContext(), AboutAppActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes") { _, _ ->
                    SessionManager.getInstance(requireContext()).logout()
                    val intent = Intent(requireContext(), SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}
