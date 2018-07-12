/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wintersky.windyreader.data.source.local;

import android.support.annotation.NonNull;

import com.wintersky.windyreader.data.Book;
import com.wintersky.windyreader.data.source.DataSource;
import com.wintersky.windyreader.util.AppExecutors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Concrete implementation of a data source as a db.
 */
@Singleton
public class LocalDataSource implements DataSource {

    private final AppExecutors mExecutors;

    @Inject
    LocalDataSource(@NonNull AppExecutors executors) {
        mExecutors = executors;
    }

    @Override
    public void getLList(LoadLListCallback callback) {
        callback.onDataNotAvailable();
    }

    @Override
    public void searchBook(String url, String key, SearchBookCallback callback) {
        //none
    }

    @Override
    public void getBList(@NonNull final LoadBListCallback callback) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Book> books = realm.where(Book.class).findAll();
        callback.onLoaded(books);
    }

    @Override
    public void getBook(final String url, final GetBookCallback callback) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Book> books = realm.where(Book.class).equalTo("url", url).findAll();
        if (books.size() > 0) {
            callback.onLoaded(books.first());
        } else {
            callback.onDataNotAvailable();
        }
    }

    @Override
    public void getCList(final String url, final LoadCListCallback callback) {
        callback.onDataNotAvailable();
    }

    @Override
    public void getChapter(String url, GetChapterCallback callback) {
        callback.onDataNotAvailable();
    }

    @Override
    public void saveBook(final Book book) {
        mExecutors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(book);
                realm.commitTransaction();
            }
        });
    }
}
