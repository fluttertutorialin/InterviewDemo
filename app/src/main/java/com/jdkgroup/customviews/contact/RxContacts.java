package com.jdkgroup.customviews.contact;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.LongSparseArray;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class RxContacts {
    private static final String[] PROJECTION = {
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.PHOTO_URI,
            ContactsContract.Data.PHOTO_THUMBNAIL_URI,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.IN_VISIBLE_GROUP
    };

    private ContentResolver mResolver;

    public static Observable<Contact> fetch(@NonNull final Context context) {
        return Observable.create(new ObservableOnSubscribe<Contact>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Contact> e) throws Exception {
                new RxContacts(context).fetch(e);
            }
        });
    }

    private RxContacts(@NonNull Context context) {
        mResolver = context.getContentResolver();
    }

    private void fetch(ObservableEmitter emitter) {
        LongSparseArray<Contact> contacts = new LongSparseArray<>();
        Cursor cursor = createCursor();
        if (cursor == null) {
            emitter.onError(new IllegalArgumentException());
            return;
        }
        cursor.moveToFirst();
        int idColumnIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
        int inVisibleGroupColumnIndex = cursor.getColumnIndex(ContactsContract.Data.IN_VISIBLE_GROUP);
        int displayNamePrimaryColumnIndex = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY);
        int starredColumnIndex = cursor.getColumnIndex(ContactsContract.Data.STARRED);
        int photoColumnIndex = cursor.getColumnIndex(ContactsContract.Data.PHOTO_URI);
        int thumbnailColumnIndex = cursor.getColumnIndex(ContactsContract.Data.PHOTO_THUMBNAIL_URI);
        int mimetypeColumnIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
        int dataColumnIndex = cursor.getColumnIndex(ContactsContract.Data.DATA1);
        while (!cursor.isAfterLast()) {
            long id = cursor.getLong(idColumnIndex);
            Contact contact = contacts.get(id, null);
            if (contact == null) {
                contact = new Contact(id);
                ColumnMapper.mapInVisibleGroup(cursor, contact, inVisibleGroupColumnIndex);
                ColumnMapper.mapDisplayName(cursor, contact, displayNamePrimaryColumnIndex);
                ColumnMapper.mapStarred(cursor, contact, starredColumnIndex);
                ColumnMapper.mapPhoto(cursor, contact, photoColumnIndex);
                ColumnMapper.mapThumbnail(cursor, contact, thumbnailColumnIndex);
                contacts.put(id, contact);
            } else {
                String mimetype = cursor.getString(mimetypeColumnIndex);
                switch (mimetype) {
                    case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE: {
                        ColumnMapper.mapEmail(cursor, contact, dataColumnIndex);
                        break;
                    }
                    case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE: {
                        ColumnMapper.mapPhoneNumber(cursor, contact, dataColumnIndex);
                        break;
                    }
                }
            }
            cursor.moveToNext();
        }
        cursor.close();
        for (int i = 0; i < contacts.size(); i++)
            emitter.onNext(contacts.valueAt(i));
        emitter.onComplete();
    }

    private Cursor createCursor() {
        return mResolver.query(
                ContactsContract.Data.CONTENT_URI,
                PROJECTION,
                null,
                null,
                ContactsContract.Data.CONTACT_ID
        );
    }
}
