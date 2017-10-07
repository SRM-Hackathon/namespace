package com.example.dingu.ctjourn;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private RecyclerView postList;
    private DatabaseReference myDBRef;
    private DatabaseReference mDatabaseUsers;
    private DatabaseReference mDatabaseLike;
    private FirebaseDatabase firebaseDatabase;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private boolean mProcessLike = false;
    private float lati,longi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
       mAuthListener = new FirebaseAuth.AuthStateListener() {
           @Override
           public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
               if(firebaseAuth.getCurrentUser()==null)
               {
                   Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
                   loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                   startActivity(loginIntent);
               }
           }
       };

        firebaseDatabase = FirebaseDatabase.getInstance();
        myDBRef = firebaseDatabase.getReference().child("Post");
        myDBRef.keepSynced(true);

        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        mDatabaseUsers.keepSynced(true);

        mDatabaseLike = FirebaseDatabase.getInstance().getReference().child("Likes");

        postList = (RecyclerView)findViewById(R.id.post_list);
        postList.setHasFixedSize(true);
        postList.setLayoutManager(new LinearLayoutManager(this));
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    lati = (float) location.getLatitude();
                    longi = (float) location.getLongitude();
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }

        };
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    }

    @Override
    protected void onStart() {
        super.onStart();

        mAuth.addAuthStateListener(mAuthListener);

        FirebaseRecyclerAdapter<Post,PostViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Post, PostViewHolder>(
                Post.class,
                R.layout.post_row,
                PostViewHolder.class,
                myDBRef

        ) {
            @Override
            protected void populateViewHolder(final PostViewHolder viewHolder, final Post model, int position) {
                final String postKey = getRef(position).getKey();

                viewHolder.setTitle(model.getTitle());
                viewHolder.setDesc(model.getDesc());
                viewHolder.setImage(getApplicationContext(),model.getVideo());
                viewHolder.setUserName(model.getUsername());
//                viewHolder.setUpvoteButton(postKey);

                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HashMap<String,Integer> votes = model.getVotes();
                        if(mAuth.getCurrentUser() != null)
                        {
                            String uid = mAuth.getCurrentUser().getUid().toString();
                            if(v.getId() == R.id.upvote)
                            {
                                votes.put(uid,1);
                                model.setVotes(votes);
                                myDBRef.child(""+model.getPostID()).setValue(model);
                                Toast.makeText(getApplicationContext(),"Up voted !!",Toast.LENGTH_SHORT).show();
                                changeRating(model,votes.get(uid));
                            }else if(v.getId() == R.id.downvote)
                            {
                                votes.put(uid,-1);
                                model.setVotes(votes);
                                myDBRef.child(""+model.getPostID()).setValue(model);
                                Toast.makeText(getApplicationContext(),"Down voted !!",Toast.LENGTH_SHORT).show();
                                changeRating(model,votes.get(uid));
                            }

                            mDatabaseUsers.child(uid).child("lastLatitude").setValue(lati);
                            mDatabaseUsers.child(uid).child("lastLongitude").setValue(longi);
                        }

                    }
                };

                viewHolder.upvoteButton.setOnClickListener(listener);
                viewHolder.downVoteButton.setOnClickListener(listener);
            }
        };
        postList.setAdapter(firebaseRecyclerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       if(item.getItemId() == R.id.action_add)
            startActivity(new Intent(MainActivity.this,PostActivity.class));
        else if(item.getItemId() == R.id.action_log_out)
       {
           mAuth.signOut();
           startActivity(new Intent(MainActivity.this, LoginActivity.class)); //Go back to home page
           finish();
       }
        return super.onOptionsItemSelected(item);
    }

    private Boolean exit = false;
    @Override
    public void onBackPressed() {
        if (exit) {
            finish(); // finish activity
        } else {
            Toast.makeText(this, "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);

        }

    }

    public static class PostViewHolder extends RecyclerView.ViewHolder{

        View mView;
        ImageButton upvoteButton;
        ImageButton downVoteButton;
        DatabaseReference mDatabaseRef;
        FirebaseAuth newAuth;
         String videoURL=null;
        public PostViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            ImageView thumbnail = (ImageView)mView.findViewById(R.id.post_image);
            upvoteButton =(ImageButton) mView.findViewById(R.id.upvote);
            downVoteButton = (ImageButton)mView.findViewById(R.id.downvote);

            mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Likes");
            newAuth = FirebaseAuth.getInstance();
            mDatabaseRef.keepSynced(true);
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mView.getContext(),videoPlayer.class);
                    intent.putExtra("URL",videoURL);
                    mView.getContext().startActivity(intent);

                }
            });
        }

        public void setTitle(String title)
        {
            TextView post_title = (TextView) mView.findViewById(R.id.post_title);
            post_title.setText(title);
        }

        public void setDesc(String desc)
        {
            TextView post_desc = (TextView)mView.findViewById(R.id.post_desc);
            post_desc.setText(desc);
        }

        public void setImage(Context context, String image)
        {
            ImageView postImage = (ImageView)mView.findViewById(R.id.post_image);
//            Picasso.with(context).load(image).into(postImage);
            videoURL = image;
            ImageLoadingAsynTask mytask = new ImageLoadingAsynTask(videoURL,postImage );
            mytask.execute();

        }

        public void setUserName(String userName)
        {
            TextView post_username = (TextView)mView.findViewById(R.id.post_username);
            post_username.setText("posted by "+userName);
        }

        public void setUpvoteButton(final String postKey)
        {
            mDatabaseRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean userLoggedIn ;
                    if (newAuth.getCurrentUser() != null) {
                        if(dataSnapshot.child(postKey).hasChild(newAuth.getCurrentUser().getUid()))
                        {
                            upvoteButton.setImageResource(R.mipmap.blue_like);
                        }
                        else
                        {
                            upvoteButton.setImageResource(R.mipmap.grey_like);
                        }
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

//        public void setDownVoteButton(final String postKey)
//        {
//            mDatabaseRef.addValueEventListener(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    boolean userLoggedIn ;
//                    if (newAuth.getCurrentUser() != null) {
//                        if(dataSnapshot.child(postKey).hasChild(newAuth.getCurrentUser().getUid()))
//                        {
//                            downVoteButton.setImageResource(R.mipmap.blue_like);
//                        }
//                        else
//                        {
//                            downVoteButton.setImageResource(R.mipmap.grey_like);
//                        }
//                    }
//
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//
//                }
//            });
//        }
    }
    private  void changeRating(final Post post, final int value){
        mDatabaseUsers.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long reputation = (long)dataSnapshot.child("reputation").getValue();
                    post.setRating(post.getRating()+value*reputation);
                myDBRef.child(""+post.getPostID()).setValue(post);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
