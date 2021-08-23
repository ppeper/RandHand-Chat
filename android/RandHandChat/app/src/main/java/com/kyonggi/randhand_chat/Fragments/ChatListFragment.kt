package com.kyonggi.randhand_chat.Fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kyonggi.randhand_chat.Adapter.ChatRoomAdapter
import com.kyonggi.randhand_chat.Database.*
import com.kyonggi.randhand_chat.Domain.Chat.ChatInfo
import com.kyonggi.randhand_chat.Domain.User.ResponseUser
import com.kyonggi.randhand_chat.Retrofit.IRetrofit.IRetrofitChat
import com.kyonggi.randhand_chat.Retrofit.IRetrofit.IRetrofitUser
import com.kyonggi.randhand_chat.Retrofit.ServiceURL
import com.kyonggi.randhand_chat.Util.AppUtil
import com.kyonggi.randhand_chat.databinding.FragmentChatsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ChatListFragment : Fragment() {
    private lateinit var retrofit: Retrofit
    private lateinit var supplementServiceUser: IRetrofitUser
    private lateinit var supplementServiceChat: IRetrofitChat

    private lateinit var chatBinding: FragmentChatsBinding
    private var chatRoomList: MutableList<ChatRoomTable> = mutableListOf()


    // 채팅방 정보에대한 sessionId 와 List<String>
    private lateinit var chatInfo: List<ChatInfo>

    /**
     * ChatRoom Database
     */
    private lateinit var chatRoomDatabase: ChatRoomDatabase
    private lateinit var chatDAO: ChatRoomDAO
    private lateinit var messageDAO: MessageDAO

    companion object {
        const val TAG : String = "로그"
        // 채팅방 정보에대한 Fragment 생성
        fun newInstance(): ChatListFragment {
            return ChatListFragment()
        }
    }

    // 메모리에 올라갔을때
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "ChatFragment -onCreate() called")

        chatRoomDatabase = context?.let { ChatRoomDatabase.getInstance(it) }!!
        chatDAO = chatRoomDatabase.roomChatRoomDAO()    // 채팅방에대한 데이터베이스
        messageDAO = chatRoomDatabase.roomMessageDAO()  // 메시지에 대한 데이터베이스
        initUserRetrofit()
        initChatRetrofit()
    }

    private fun getChatRoomInfo(supplementServiceChat: IRetrofitChat, token: String, userId: String) {
        supplementServiceChat.getChatRoomInfoByUserId(token, userId).enqueue(object : Callback<List<ChatInfo>> {
            override fun onResponse(call: Call<List<ChatInfo>>, response: Response<List<ChatInfo>>) {
                val list = response.body()
                if (list != null) {
                    chatInfo = list
                    // 채팅하는 유저 아이디
                    for (info in chatInfo) {
                        val chatId = info.userIds.filter { it != AppUtil.prefs.getString("userId", null) }[0]
                        getUserInfo(supplementServiceUser, chatId, info)
                    }

                    chatRoomList = chatDAO.getAll() as MutableList<ChatRoomTable>
                } else {
                    chatInfo = listOf()
                }
                // 어뎁터 붙여주기
                chatBinding.chatRoom.adapter = ChatRoomAdapter(chatRoomList)
            }

            override fun onFailure(call: Call<List<ChatInfo>>, t: Throwable) {
                Log.d("ERROR", "오류")
            }

        })

    }

    // Fragment 를 안고 있는 Activity 에 붙였을때
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "ChatFragment -onAttach() called")
    }

    // View 가 생성되었을때
    // Fragment 와 레이아웃을 연결시켜준다
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "ChatFragment -onCreateView() called")
//        val view = inflater.inflate(R.layout.fragment_chattings, container, false )
        chatBinding = FragmentChatsBinding.inflate(inflater, container, false)
        return chatBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(FriendFragment.TAG, "ChatFragment -onViewCreated() called")





        with(chatBinding) {
            // 리사이클러뷰 설정
            chatRoom.apply {
                // 세로로 만들어주기
                layoutManager = LinearLayoutManager(activity)
                // 어뎁터 성능을 위해서 추가
                setHasFixedSize(true)

                // TEST
                val token = AppUtil.prefs.getString("token",null)
                val userId = AppUtil.prefs.getString("userId",null)
                getChatRoomInfo(supplementServiceChat, token, userId)
            }
        }
    }

    private fun getUserInfo(supplementServiceUser: IRetrofitUser, chatId: String, chatInfo: ChatInfo) {
        val token = AppUtil.prefs.getString("token",null)
        val userId = AppUtil.prefs.getString("userId", null)
        supplementServiceUser.getUserInfo(token, userId, chatId).enqueue(object :
            Callback<ResponseUser> {
            override fun onResponse(call: Call<ResponseUser>, response: Response<ResponseUser>) {
                val info = response.body()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

                val syncTime = LocalDateTime.parse(chatInfo.syncTime, formatter)
                /**
                 *                        테스트용 데이터베이스 데이터
                 *          채팅하고 있는 상대방의 seesionID, 상대방ID와 프로필정보를 넣어준다
                 */
                chatDAO.insertRoomInfo(
                    ChatRoomTable(chatInfo.sessionId,
                        chatId,info?.name!!, info.picture,syncTime,"테스트용")
                )
            }

            override fun onFailure(call: Call<ResponseUser>, t: Throwable) {
            }

        })
    }

    private fun initUserRetrofit() {
        retrofit = ServiceURL.getInstance()
        supplementServiceUser = retrofit.create(IRetrofitUser::class.java)
    }

    private fun initChatRetrofit() {
        retrofit = ServiceURL.getInstance()
        supplementServiceChat = retrofit.create(IRetrofitChat::class.java)
    }
}