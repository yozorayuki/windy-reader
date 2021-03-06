package com.wintersky.windyreader.read;

import android.support.annotation.NonNull;

import com.wintersky.windyreader.data.Book;
import com.wintersky.windyreader.data.Chapter;
import com.wintersky.windyreader.data.source.DataSource;
import com.wintersky.windyreader.data.source.Repository;

import java.util.Date;

import javax.inject.Inject;

import io.realm.Realm;

import static com.wintersky.windyreader.util.LogUtil.LOG;

public class ReadPresenter implements ReadContract.Presenter {

    private final Repository mRepository;
    private final String mUrl;
    private ReadContract.View mView;
    private boolean isFirst = true;

    private Book mBook;

    @Inject
    ReadPresenter(Repository repository, String url) {
        mRepository = repository;
        mUrl = url;
    }

    @Override
    public void takeView(ReadContract.View view) {
        mView = view;
        start();
    }

    @Override
    public void dropView() {
        mView = null;
    }

    private void start() {
        if (isFirst) {
            isFirst = false;
        } else {
            return;
        }
        mRepository.getBook(mUrl, new DataSource.GetBookCallback() {
            @Override
            public void onLoaded(@NonNull final Book book) {
                mBook = book;
                book.getRealm().executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(@NonNull Realm realm) {
                        book.setLastRead(new Date());
                        book.setHasNew(false);
                    }
                });
                if (mView != null) {
                    mView.setBook(book);
                }
            }

            @Override
            public void onFailed(@NonNull Throwable error) {
                LOG("Read - get book fail: " + mUrl, error);
            }
        });
    }

    @Override
    public void saveReadIndex(final float index) {
        if (mBook == null) {
            return;
        }
        mBook.getRealm().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                Chapter chapter = mBook.getCatalog().get((int) index);
                if (chapter != null) {
                    chapter.setRead(true);
                }
                mBook.setIndex(index);
            }
        });
    }

    @Override
    public void getContent(final Chapter chapter, final float progress) {
        mRepository.getContent(chapter, new DataSource.GetContentCallback() {
            @Override
            public void onLoaded(@NonNull String content) {
                if (mView != null) {
                    mView.setContent(chapter, content.trim(), progress);
                }
            }

            @Override
            public void onFailed(@NonNull Throwable error) {
                LOG(error);
                if (mView != null) {
                    mView.setContent(chapter, error.getMessage(), progress);
                }
            }
        });
    }
}
