/*
        Copyright 2018 Kurt Wanliss

        All rights reserved under the copyright laws of the United States
        and applicable international laws, treaties, and conventions.

        You may freely redistribute and use this sample code, with or
        without modification, provided you include the original copyright
        notice and use restrictions.

*/

package com.wanliss.kurt.hmByElla;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.wanliss.kurt.hmByElla.DTO.StoreDisplayDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GalleryActivity extends AppCompatActivity
        implements GalleryAdapter.ListItemClickListener, GlobalLogin.LoginListener {
    private static final String TAG = GalleryActivity.class.getSimpleName();
    private final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private final List<StoreDisplayDTO> mImageToUpload = new ArrayList<>();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private DatabaseReference mGalleryDbImagesRef = null;
    private DatabaseReference mGroupImagesDbRef = null;
    private StorageReference mLargeStoreGalery = null;
    private StorageReference mThumbnailStoreGallery = null;
    private StorageReference mThumbnailStoreGroupImages = null;
    private StorageReference mGroupStoreImages = null;
    private SwipeController swipeController = null;
    private RecyclerView mGalleryRecyclerView;
    private GalleryAdapter mStoreRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.cptoolLay);
        final Typeface tf = Typeface.createFromAsset(this.getAssets(), getString(R.string.charmonman_file_name));
        // collapsingToolbarLayout.setCollapsedTitleTypeface(tf);
        collapsingToolbarLayout.setExpandedTitleTypeface(tf);

        GlobalLogin.initialize_drawer(this);

        CheckInternet iTest = new CheckInternet(this);
        iTest.execute();

        FloatingActionButton fab = findViewById(R.id.fab);
        //noinspection RestrictedApi
        fab.setVisibility(View.GONE);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, 1);
        mGalleryRecyclerView = findViewById(R.id.rv_posters);
        mGalleryRecyclerView.setHasFixedSize(true);
        mGalleryRecyclerView.setLayoutManager(layoutManager);

        mGalleryDbImagesRef = mDatabase.getReference(getString(R.string.thumbnail_galery_db_location));
        mGroupImagesDbRef = mDatabase.getReference(getString(R.string.thumbnail_GroupImages_db_location));

        mLargeStoreGalery = storage.getReference(getString(R.string.storage_large_gallery));
        mGroupStoreImages = storage.getReference(getString(R.string.storage_group_images));
        mThumbnailStoreGallery = storage.getReference(getString(R.string.storage_thumbnails_gallery));
        mThumbnailStoreGroupImages = storage.getReference(getString(R.string.storage_thumbnails_group_images));

        mGalleryDbImagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                getAllClients(dataSnapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                getAllClients(dataSnapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                clientDeleted(dataSnapshot);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, getString(R.string.gallery_error_msg), databaseError.toException());
            }
        });

    }


    private void clientDeleted(DataSnapshot dataSnapshot) {
        StoreDisplayDTO store = dataSnapshot.getValue(StoreDisplayDTO.class);
        for (int i = 0; i < mImageToUpload.size(); i++) {
            if (mImageToUpload.get(i).getPath().equals(Objects.requireNonNull(store).getPath())) {
                mImageToUpload.remove(i);
            }
        }
        mStoreRecyclerViewAdapter.notifyDataSetChanged();
        mStoreRecyclerViewAdapter = new GalleryAdapter(mImageToUpload, GalleryActivity.this);
        mGalleryRecyclerView.setAdapter(mStoreRecyclerViewAdapter);
    }

    private void getAllClients(DataSnapshot dataSnapshot) {
        StoreDisplayDTO store = dataSnapshot.getValue(StoreDisplayDTO.class);
        mImageToUpload.add(store);
        mStoreRecyclerViewAdapter = new GalleryAdapter(mImageToUpload, GalleryActivity.this);
        mGalleryRecyclerView.setAdapter(mStoreRecyclerViewAdapter);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onListItemClick(final StoreDisplayDTO clickedImage) {

        mGroupImagesDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(clickedImage.getName()).exists()) {
                    Context context = GalleryActivity.this;
                    Class destinationActivity = ImagesActivity.class;

                    Intent startChildActivityIntent = new Intent(context, destinationActivity);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(getString(R.string.gallery_intent_put_key), clickedImage);
                    startChildActivityIntent.putExtras(bundle);
                    startActivity(startChildActivityIntent);
                } else {
                    Snackbar.make(mGalleryRecyclerView, getString(R.string.gallery_no_sub_images), Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.gallery_action), null).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, getString(R.string.gallery_error_msg), databaseError.toException());
            }
        });
    }

    @Override
    public void onCheckedLogIn(GlobalLogin.dataSet admin) {
        FloatingActionButton fab = findViewById(R.id.fab);

        if (admin == GlobalLogin.dataSet.SET_TRUE) {

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(GalleryActivity.this, UploadImageActivity.class);
                    GalleryActivity.this.startActivity(intent);
                }
            });
            //noinspection RestrictedApi
            fab.setVisibility(View.VISIBLE);

            swipeController = new SwipeController(new SwipeControllerActions() {
                @Override
                public void onRightClicked(int position) {
                    validateDeleteDialog(mImageToUpload.get(position));
                }

                @Override
                public void onLeftClicked(int position) {
                    Context context = GalleryActivity.this;
                    Class destinationActivity = UploadImageActivity.class;
                    Intent startChildActivityIntent = new Intent(context, destinationActivity);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(getString(R.string.gallery_intent_put_key), mImageToUpload.get(position));
                    startChildActivityIntent.putExtras(bundle);
                    startActivity(startChildActivityIntent);
                }
            });

            ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
            itemTouchhelper.attachToRecyclerView(mGalleryRecyclerView);

            mGalleryRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    swipeController.onDraw(c);
                }
            });
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    private void validateDeleteDialog(final StoreDisplayDTO client) {
        String full_name = client.getName();
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.confirm_delete_dialog);
        TextView client_name = dialog.findViewById(R.id.file_name);
        client_name.setText(full_name);

        Button yes = dialog.findViewById(R.id.yes);
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.hide();
                deleteClient(client);
            }
        });

        Button no = dialog.findViewById(R.id.no);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.hide();
            }
        });
        dialog.show();
    }

    private void deleteClient(final StoreDisplayDTO client) {
        Query queryRef = mGroupImagesDbRef.child(client.getName()).orderByKey().limitToFirst(200);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    final StoreDisplayDTO store = dataSnapshot.getValue(StoreDisplayDTO.class);
                    mGroupStoreImages.child(client.getName()).child(Objects.requireNonNull(store).getName()).delete();
                    mThumbnailStoreGroupImages.child(client.getName()).child(store.getName()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            mThumbnailStoreGroupImages.child(client.getName()).child(store.getName()).delete();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // File not found
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, getString(R.string.gallery_error_msg), databaseError.toException());
            }
        });

        mGroupStoreImages.child(client.getName()).delete();
        mThumbnailStoreGroupImages.child(client.getName()).delete();

        mLargeStoreGalery.child(client.getName()).delete();
        mThumbnailStoreGallery.child(client.getName()).delete();

        mGroupImagesDbRef.child(client.getName()).removeValue();
        mGalleryDbImagesRef.child(client.getName()).removeValue();

        Snackbar.make(mGalleryRecyclerView, getString(R.string.gallery_deleted_msg), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.gallery_action), null).show();
    }

}