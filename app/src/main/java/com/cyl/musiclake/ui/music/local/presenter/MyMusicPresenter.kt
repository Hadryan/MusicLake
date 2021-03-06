package com.cyl.musiclake.ui.music.local.presenter

import com.cyl.musiclake.MusicApp
import com.cyl.musiclake.RxBus
import com.cyl.musiclake.api.PlaylistApiServiceImpl
import com.cyl.musiclake.base.BasePresenter
import com.cyl.musiclake.common.Constants
import com.cyl.musiclake.data.DownloadLoader
import com.cyl.musiclake.data.PlayHistoryLoader
import com.cyl.musiclake.data.SongLoader
import com.cyl.musiclake.data.db.Music
import com.cyl.musiclake.data.db.Playlist
import com.cyl.musiclake.event.LoginEvent
import com.cyl.musiclake.event.MetaChangedEvent
import com.cyl.musiclake.event.PlaylistEvent
import com.cyl.musiclake.net.ApiManager
import com.cyl.musiclake.net.RequestCallBack
import com.cyl.musiclake.ui.music.local.contract.MyMusicContract
import com.cyl.musiclake.ui.my.user.UserStatus
import com.cyl.musiclake.utils.ToastUtils
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*
import javax.inject.Inject

/**
 * Created by yonglong on 2018/1/6.
 */

class MyMusicPresenter @Inject
constructor() : BasePresenter<MyMusicContract.View>(), MyMusicContract.Presenter {
    private val playlists = ArrayList<Playlist>()

    init {
        /**登陆成功重新设置用户新 */
        RxBus.getInstance().register(MetaChangedEvent::class.java).subscribe { event -> updateHistory() }
        RxBus.getInstance().register(LoginEvent::class.java).subscribe { event -> loadPlaylist() }
        RxBus.getInstance().register(PlaylistEvent::class.java).subscribe {
            when (it.type) {
                Constants.PLAYLIST_CUSTOM_ID -> loadPlaylist()
                Constants.PLAYLIST_LOVE_ID -> updateFavorite()
                Constants.PLAYLIST_HISTORY_ID -> updateHistory()
                Constants.PLAYLIST_DOWNLOAD_ID -> updateDownload()
            }
        }
    }

    /**
     * 更新播放历史
     */
    private fun updateHistory() {
        doAsync {
            val data = PlayHistoryLoader.getPlayHistory()
            uiThread {
                mView?.showHistory(data)
            }
        }
    }

    /**
     * 更新播放历史
     */
    private fun updateLocal() {
        doAsync {
            val data = SongLoader.getLocalMusic(mView.context)
            uiThread {
                mView?.showSongs(data)
            }
        }
    }

    /**
     * 更新本地歌单
     */
    private fun updateFavorite() {
        doAsync {
            val data = SongLoader.getFavoriteSong()
            uiThread {
                mView?.showLoveList(data)
            }
        }
    }


    /**
     * 更新本地歌单
     */
    private fun updateDownload() {
        doAsync {
            val data = DownloadLoader.getDownloadList()
            uiThread {
                mView?.showDownloadList(data)
            }
        }
    }

    override fun loadSongs() {
        updateLocal()
        updateHistory()
        updateFavorite()
        updateDownload()
    }

    override fun loadPlaylist() {
        val mIsLogin = UserStatus.getstatus(MusicApp.getAppContext())
        if (mIsLogin) {
            ApiManager.request(PlaylistApiServiceImpl.getPlaylist(), object : RequestCallBack<MutableList<Playlist>> {
                override fun success(result: MutableList<Playlist>) {
                    result.forEach {
                        it.pid = it.id.toString()
                        it.type = 1
                    }
                    mView?.showPlaylist(result)
                    if (result.isEmpty()) {
                        mView?.showEmptyView()
                    }
                }

                override fun error(msg: String) {
                    ToastUtils.show(msg)
                    mView?.showEmptyView()
                }
            })
        } else {
            playlists.clear()
            mView?.showPlaylist(playlists)
            if (playlists.size == 0) {
                mView?.showEmptyView()
            }
        }
    }
}
