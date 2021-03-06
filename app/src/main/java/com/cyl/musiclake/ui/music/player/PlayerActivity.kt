package com.cyl.musiclake.ui.music.player

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.view.ViewPager
import android.support.v7.graphics.Palette
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.cyl.musiclake.R
import com.cyl.musiclake.api.AddPlaylistUtils
import com.cyl.musiclake.api.MusicUtils
import com.cyl.musiclake.base.BaseActivity
import com.cyl.musiclake.common.Constants
import com.cyl.musiclake.common.TransitionAnimationUtils
import com.cyl.musiclake.data.db.Music
import com.cyl.musiclake.player.MusicPlayerService
import com.cyl.musiclake.player.PlayManager
import com.cyl.musiclake.ui.UIUtils
import com.cyl.musiclake.ui.music.dialog.downloadMusic
import com.cyl.musiclake.ui.music.local.adapter.MyPagerAdapter
import com.cyl.musiclake.ui.music.playqueue.PlayQueueDialog
import com.cyl.musiclake.utils.ColorUtil
import com.cyl.musiclake.utils.FormatUtil
import com.cyl.musiclake.utils.LogUtil
import com.cyl.musiclake.view.DepthPageTransformer
import com.cyl.musiclake.view.LyricView
import com.cyl.musiclake.view.MultiTouchViewPager
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.frag_player_coverview.*

class PlayerActivity : BaseActivity<PlayPresenter>(), PlayContract.View {

    private var playingMusic: Music? = null
    private lateinit var coverView: View
    private lateinit var lyricView: View
    private val viewPagerContent = mutableListOf<View>()
    private var mLyricView: LyricView? = null


    override fun showNowPlaying(music: Music?) {
        if (music == null) finish()

        playingMusic = music
        //更新标题
        titleIv.text = music?.title
        subTitleTv.text = music?.artist

        updateMusicType(playingMusic?.type)
        //更新收藏状态
        music?.isLove?.let {
            collectIv.setImageResource(if (it) R.drawable.item_favorite_love else R.drawable.item_favorite)
        }

    }

    override fun getLayoutResID(): Int {
        return R.layout.activity_player
    }

    override fun setToolbarTitle(): String {
        return ""
    }

    override fun initView() {
        detailView.animation = moveToViewLocation()
        updatePlayMode()
    }

    override fun updatePlayMode() {
        UIUtils.updatePlayMode(playModeIv, false)
    }

    override fun initData() {
        setupViewPager(viewPager)
        initAlbumPic(coverView.findViewById(R.id.civ_cover))
        mPresenter?.updateNowPlaying(PlayManager.getPlayingMusic())
        //初始加載歌詞
        //更新播放状态
        PlayManager.isPlaying().let {
            updatePlayStatus(it)
        }
        showLyric(MusicPlayerService.lyric, true)
        updateMusicType(playingMusic?.type)
    }

    override fun listener() {
        super.listener()
        progressSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let {
                    PlayManager.seekTo(it)
                    mLyricView?.setCurrentTimeMillis(it.toLong())
                }
            }

        })
        playPauseIv.setOnClickListener {
            PlayManager.playPause()
        }

    }

    override fun initInjector() {
        mActivityComponent.inject(this)
    }


    fun nextPlay(view: View?) {
        PlayManager.next()
    }

    fun prevPlay(view: View?) {
        PlayManager.prev()
    }

    fun changePlayMode(view: View?) {
        UIUtils.updatePlayMode(view as ImageView, true)
    }

    /**
     * 打开播放队列
     */
    fun openPlayQueue(view: View?) {
        val fm = supportFragmentManager
        PlayQueueDialog.newInstance().show(fm, "fragment_bottom_dialog")
    }

    /**
     * 歌曲收藏
     */
    fun collectMusic(view: View?) {
        UIUtils.collectMusic(view as ImageView, playingMusic)
    }

    /**
     * 添加到歌單
     */
    fun addToPlaylist(view: View?) {
        AddPlaylistUtils.getPlaylist(this, playingMusic)
    }

    /**
     * 分享歌曲
     */
    fun shareMusic(view: View?) {
        MusicUtils.qqShare(this, PlayManager.getPlayingMusic())
    }

    fun downloadMusic(view: View?) {
        downloadMusic(playingMusic)
    }

    override fun setPlayingBitmap(albumArt: Bitmap?) {
        coverView.findViewById<ImageView>(R.id.civ_cover).setImageBitmap(albumArt)
    }

    override fun setPlayingBg(albumArt: Drawable?) {
        //加载背景图过度
        TransitionAnimationUtils.startChangeAnimation(playingBgIv, albumArt)
    }

    override fun updatePlayStatus(isPlaying: Boolean) {
        if (isPlaying) playPauseIv.play() else playPauseIv.pause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isPlaying && !coverAnimator.isStarted) {
                coverAnimator.start()
            } else if (isPlaying && coverAnimator.isPaused) {
                coverAnimator.resume()
            } else {
                coverAnimator.pause()
            }
        }
    }

    private var mPalette: Palette? = null
    private var mSwatch: Palette.Swatch? = null

    override fun setPalette(palette: Palette?) {
        if (palette == null) return
        mPalette = palette
        mSwatch = palette.vibrantSwatch
        mSwatch?.let {
            subTitleTv.setTextColor(it.bodyTextColor)
            //set icon color
            val blackWhiteColor = ColorUtil.getBlackWhiteColor(it.rgb)
            titleIv.setTextColor(blackWhiteColor)
            progressTv.setTextColor(blackWhiteColor)
            durationTv.setTextColor(blackWhiteColor)
            playModeIv.setColorFilter(blackWhiteColor)
            prevPlayIv.setColor(blackWhiteColor)
            nextPlayIv.setColor(blackWhiteColor)
            playQueueIv.setColor(blackWhiteColor)
            downloadIv.setColor(blackWhiteColor)
            shareIv.setColor(blackWhiteColor)
            playlistAddIv.setColor(blackWhiteColor)
            playPauseIv.btnColor = blackWhiteColor
        }
    }

    override fun showLyric(lyric: String?, init: Boolean) {
        if (init) {
            //初始化歌词配置
            mLyricView?.setTouchable(true)
            mLyricView?.setOnPlayerClickListener { progress, _ ->
                PlayManager.seekTo(progress.toInt())
                if (!PlayManager.isPlaying()) {
                    PlayManager.playPause()
                }
            }
        }
        if (lyric != null) {
            mLyricView?.setLyricContent(lyric)
        } else {
            mLyricView?.reset()
        }
    }
//
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_content, menu)
//        return super.onCreateOptionsMenu(menu)
//    }

    override fun updateProgress(progress: Long, max: Long) {
        progressSb.progress = progress.toInt()
        progressSb.max = max.toInt()
        progressTv.text = FormatUtil.formatTime(progress)
        durationTv.text = FormatUtil.formatTime(max)

        mLyricView?.setCurrentTimeMillis(progress)
    }

    private fun setupViewPager(viewPager: MultiTouchViewPager) {
        //初始化View
        coverView = LayoutInflater.from(this).inflate(R.layout.frag_player_coverview, viewPager, false)
        lyricView = LayoutInflater.from(this).inflate(R.layout.frag_player_lrcview, viewPager, false)
        mLyricView = lyricView.findViewById(R.id.lyricShow)
        viewPagerContent.add(coverView)
        viewPagerContent.add(lyricView)

        val mAdapter = MyPagerAdapter(viewPagerContent)
        viewPager.adapter = mAdapter
        viewPager.setPageTransformer(false, DepthPageTransformer())
        viewPager.offscreenPageLimit = 2
        viewPager.currentItem = 0
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                LogUtil.d("PlayControlFragment", "--$position")

            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    private fun moveToViewLocation(): TranslateAnimation {
        val mHiddenAction = TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_SELF,
                1.0f, Animation.RELATIVE_TO_SELF, 0.0f)
        mHiddenAction.duration = 300
        return mHiddenAction
    }


    private lateinit var coverAnimator: ObjectAnimator

    /**
     * 初始化旋转动画
     */
    private fun initAlbumPic(view: View?) {
        if (view == null) return
        coverAnimator = ObjectAnimator.ofFloat(view, "rotation", 0F, 359F)
        coverAnimator.duration = (20 * 1000).toLong()
        coverAnimator.repeatCount = -1
        coverAnimator.repeatMode = ObjectAnimator.RESTART
        coverAnimator.interpolator = LinearInterpolator()
    }


    /**
     * 更新歌曲類型
     */
    private fun updateMusicType(type: String?) {
        val value: String? = when (type) {
            Constants.QQ -> {
                getString(R.string.res_qq)
            }
            Constants.BAIDU -> {
                getString(R.string.res_baidu)
            }
            Constants.NETEASE -> {
                getString(R.string.res_wangyi)
            }
            Constants.XIAMI -> {
                getString(R.string.res_xiami)
            }
            else -> {
                getString(R.string.res_local)
            }
        }
        value?.let {
            coverView.findViewById<TextView>(R.id.tv_source).text = value
        }
    }

}
