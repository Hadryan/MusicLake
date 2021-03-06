package com.cyl.musiclake.ui.music.local.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.cyl.musiclake.R;
import com.cyl.musiclake.base.BaseLazyFragment;
import com.cyl.musiclake.common.Constants;
import com.cyl.musiclake.data.db.Music;
import com.cyl.musiclake.player.PlayManager;
import com.cyl.musiclake.ui.music.dialog.PopupDialogFragment;
import com.cyl.musiclake.ui.music.local.adapter.SongAdapter;
import com.cyl.musiclake.ui.music.local.contract.SongsContract;
import com.cyl.musiclake.ui.music.local.presenter.SongsPresenter;
import com.cyl.musiclake.view.ItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;

/**
 * 功能：本地歌曲列表
 * 作者：yonglong on 2016/8/10 20:49
 * 邮箱：643872807@qq.com
 * 版本：2.5
 */
public class SongsFragment extends BaseLazyFragment<SongsPresenter> implements SongsContract.View {
    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    View mViewHeader;
    ImageView mReloadLocal;
    TextView mSongNum;
    private SongAdapter mAdapter;
    private List<Music> musicList = new ArrayList<>();

    public static SongsFragment newInstance() {
        Bundle args = new Bundle();
        SongsFragment fragment = new SongsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_recyclerview_notoolbar;
    }

    @Override
    public void initViews() {
        mSwipeRefreshLayout.setColorSchemeColors(Color.RED, Color.BLUE, Color.GREEN);
        mAdapter = new SongAdapter(musicList);
        mAdapter.openLoadAnimation(BaseQuickAdapter.SCALEIN);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new ItemDecoration(mFragmentComponent.getActivity(), ItemDecoration.VERTICAL_LIST));
        mAdapter.bindToRecyclerView(mRecyclerView);
        initHeaderView();
        mAdapter.addHeaderView(mViewHeader);
    }

    @Override
    protected void initInjector() {
        mFragmentComponent.inject(this);
    }

    @Override
    protected void listener() {
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mPresenter.loadSongs(false);
        });
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            if (view.getId() != R.id.iv_more) {
                PlayManager.play(position, musicList, Constants.PLAYLIST_LOCAL_ID);
                mAdapter.notifyDataSetChanged();
            }
        });
        mAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            Music music = (Music) adapter.getItem(position);
            PopupDialogFragment.Companion.newInstance(music)
                    .show((AppCompatActivity) mFragmentComponent.getActivity());
//            PopupMenu popupMenu = new PopupMenu(getContext(), view);
//            popupMenu.setOnMenuItemClickListener(item -> {
//                switch (item.getItemId()) {
//                    case R.id.popup_song_play:
//                        PlayManager.play(position, musicList, Constants.PLAYLIST_LOCAL_ID);
//                        break;
//                    case R.id.popup_song_detail:
//                        ShowDetailDialog.newInstance((Music) adapter.getItem(position))
//                                .show(getChildFragmentManager(), getTag());
//                        break;
//                    case R.id.popup_song_goto_album:
//                        LogUtil.e("album", music.toString() + "");
//                        NavigationHelper.INSTANCE.navigateToAlbum(getActivity(),
//                                music.getAlbumId(),
//                                music.getAlbum(), null);
//                        break;
//                    case R.id.popup_song_goto_artist:
//                        NavigationHelper.INSTANCE.navigateToArtist(getActivity(),
//                                music.getArtistId(),
//                                music.getArtist(), null);
//                        break;
//                    case R.id.popup_song_addto_queue:
//                        AddPlaylistDialog.newInstance(music).show(getChildFragmentManager(), "ADD_PLAYLIST");
//                        break;
//                    case R.id.popup_song_delete:
//                        new MaterialDialog.Builder(getContext())
//                                .title("警告")
//                                .content("是否删除这首歌曲？")
//                                .onPositive((dialog, which) -> {
//                                    FileUtils.delFile(musicList.get(position).getUri());
//                                    SongLoader.INSTANCE.removeSong(musicList.get(position));
//                                    musicList.remove(position);
//                                    mAdapter.setNewData(musicList);
//                                })
//                                .positiveText("确定")
//                                .negativeText("取消")
//                                .show();
//                        break;
//                }
//                return false;
//            });
//            popupMenu.inflate(R.menu.popup_song);
//            popupMenu.show();
        });
    }

    @Override
    public void onLazyLoad() {
        mPresenter.loadSongs(false);
    }


    private void initHeaderView() {
        mViewHeader = LayoutInflater.from(mFragmentComponent.getActivity()).inflate(R.layout.header_local_list, null);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mReloadLocal = mViewHeader.findViewById(R.id.reloadIv);
        mViewHeader.setLayoutParams(params);
        mReloadLocal.setOnClickListener(v -> {
            showLoading();
            mPresenter.loadSongs(true);
        });
        mViewHeader.setOnClickListener(v -> {
            if (musicList.size() == 0) return;
            int id = new Random().nextInt(musicList.size());
            PlayManager.play(id, musicList, Constants.PLAYLIST_LOCAL_ID);
        });
    }

    @Override
    public void showSongs(List<Music> songList) {
        musicList.clear();
        musicList.addAll(songList);
        mAdapter.setNewData(songList);

        hideLoading();
    }

    @Override
    public void showLoading() {
        super.showLoading();
    }

    @Override
    public void hideLoading() {
        super.hideLoading();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void setEmptyView() {
        mAdapter.setEmptyView(R.layout.view_song_empty);
    }
}
