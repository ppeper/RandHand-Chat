package com.kyonggi.randhand_chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kyonggi.randhand_chat.Adapter.BlockedUserAdapter
import com.kyonggi.randhand_chat.Domain.ResponseUser
import com.kyonggi.randhand_chat.Retrofit.IRetrofit
import com.kyonggi.randhand_chat.Retrofit.Service.ServiceUser
import com.kyonggi.randhand_chat.Util.AppUtil
import com.kyonggi.randhand_chat.databinding.ActivityBlockedFriendBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class BlockedFriendActivity : AppCompatActivity() {
    val binding by lazy { ActivityBlockedFriendBinding.inflate(layoutInflater) }

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: IRetrofit
    private var blockedList: MutableList<ResponseUser> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initRetrofit()

        getBlockedList(supplementService)
        binding.blockedRecycler.layoutManager = LinearLayoutManager(this)
    }


    private fun getBlockedList(supplementService: IRetrofit) {
        supplementService.getBlockUserList(AppUtil.prefs.getString("token", null), AppUtil.prefs.getString("userId",null))
            .enqueue(object : Callback<List<ResponseUser>> {
                override fun onResponse(call: Call<List<ResponseUser>>, response: Response<List<ResponseUser>>) {
                    blockedList = response.body() as MutableList<ResponseUser>
                    // 화면의 recyclerView와 연결
                    binding.blockedRecycler.adapter = BlockedUserAdapter(blockedList)
                }

                override fun onFailure(call: Call<List<ResponseUser>>, t: Throwable) {
                }

            })
    }

    private fun initRetrofit() {
        retrofit = ServiceUser.getInstance()
        supplementService = retrofit.create(IRetrofit::class.java)
    }
}