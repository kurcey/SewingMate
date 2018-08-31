package com.wanliss.kurt.sewingmate;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.wanliss.kurt.sewingmate.DTO.ClientContactDTO;

import java.util.ArrayList;
import java.util.List;

public class ContactActivity extends AppCompatActivity implements ClientContact.callOtherFragment {

    private ClientContact mClientContactFragment;
    private ClientFMeasure mClientFMeasureFragment;
    private ClientMMeasure mClientMMeasureFragment;

    private FragmentManager mTransaction;
    private String mClientFragment;
    private String mFemaleFragment;
    private String mMaleFragment;

    private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
    private DatabaseReference mClientContactInfo = mDatabase.getReference("users/" + mUser.getUid());
    private ClientContactDTO mContact;

    private RecyclerView mContactRecyclerView;
    private ContactRecyclerViewAdapter mContactRecyclerViewAdapter;
    private List<ClientContactDTO> mAllClients;
    private LinearLayoutManager mLinearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        setTheme(R.style.AppTheme);

        mAllClients = new ArrayList<ClientContactDTO>();
        mContactRecyclerView = (RecyclerView) findViewById(R.id.contact_list);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mContactRecyclerView.setLayoutManager(mLinearLayoutManager);

        mClientContactInfo.addChildEventListener(new ChildEventListener() {
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
                clientDeletion(dataSnapshot);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        /*
        mClientFragment = getResources().getString(R.string.client_fragment);
        mFemaleFragment = getResources().getString(R.string.female_fragment);
        mMaleFragment = getResources().getString(R.string.male_fragment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTransaction = getSupportFragmentManager();

        if (findViewById(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }
            mClientContactFragment = new ClientContact();
            mClientContactFragment.setArguments(getIntent().getExtras());

            mTransaction
                    .beginTransaction()
                    .add(R.id.fragment_container, mClientContactFragment, mClientFragment)
                    .addToBackStack(null)
                    .commit();

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.contactFab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Fragment currentFragment = mTransaction.findFragmentById(R.id.fragment_container);

                    if (currentFragment.getTag() != null)
                        switch (currentFragment.getTag()) {
                        case "client_fragment":
                            ((ClientContact) currentFragment).writeClientContact();
                            break;
                        case "female_fragment":
                            break;
                        case "male_fragment":
                            break;

                    }
                }
            });
        }
        */
    }

    private void getAllClients(DataSnapshot dataSnapshot) {
        for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
            ClientContactDTO singleClient = singleSnapshot.getValue(ClientContactDTO.class);
            mAllClients.add( singleClient);
            mContactRecyclerViewAdapter = new ContactRecyclerViewAdapter(ContactActivity.this, mAllClients);
            mContactRecyclerView.setAdapter(mContactRecyclerViewAdapter);
        }
    }

    private void clientDeletion(DataSnapshot dataSnapshot){
        for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
            ClientContactDTO singleClient = singleSnapshot.getValue(ClientContactDTO.class);
            for(int i = 0; i < mAllClients.size(); i++){
                if(mAllClients.get(i).getClass().equals(singleClient)){
                    mAllClients.remove(i);
                }
            }
        }
    }

    @Override
    public void selectFragment(String otherFragment, ClientContactDTO contactInfo) {
        Bundle arguments = new Bundle();
        arguments.putSerializable("clientInfo", contactInfo);

        switch (otherFragment) {
            case "female":
                mClientFMeasureFragment = new ClientFMeasure();
                mClientFMeasureFragment.setArguments(arguments);
                mTransaction
                        .beginTransaction()
                        .replace(R.id.fragment_container, mClientFMeasureFragment, mFemaleFragment)
                        .addToBackStack(null)
                        .commit();
                break;

            case "male":
                mClientMMeasureFragment = new ClientMMeasure();
                mClientMMeasureFragment.setArguments(arguments);
                mTransaction
                        .beginTransaction()
                        .replace(R.id.fragment_container, mClientMMeasureFragment, mMaleFragment)
                        .addToBackStack(null)
                        .commit();
                break;
        }
    }
}