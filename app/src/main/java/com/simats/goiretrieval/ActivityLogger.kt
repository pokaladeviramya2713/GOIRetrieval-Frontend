package com.simats.goiretrieval

import android.content.Context
import android.util.Log
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.SignupResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ActivityLogger {
    fun log(context: Context, type: String, detail: String, docId: String? = null) {
        val userId = SessionManager.getInstance(context).getUserId()

        if (userId == -1) return


        val request = com.simats.goiretrieval.api.ActivityLogRequest(
            userId = userId,
            type = type,
            detail = detail,
            docId = docId
        )
        
        RetrofitClient.instance.logActivity(request).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                if (response.isSuccessful) {
                    Log.d("ActivityLogger", "Activity logged: $type - $detail")
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                Log.e("ActivityLogger", "Failed to log activity", t)
            }
        })
    }
}
