package nz.co.ninjastudios.flixfinder;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.Builders;
import com.paginate.Paginate;
import com.paginate.recycler.LoadingListItemCreator;
import com.paginate.recycler.LoadingListItemSpanLookup;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static android.renderscript.Sampler.Value.LINEAR;
import static android.widget.GridLayout.VERTICAL;
import static android.widget.LinearLayout.HORIZONTAL;

public class ResultListActivity extends AppCompatActivity{

    private ProgressDialog progressDialog;
    private String api_key = "5549383f29245415046c05c039ef2009";
    private Gson gson;
    private Context context;

    private RecyclerView recyclerViewResults;
    private ShowsAdapter adapter;

    private boolean loading = false;
    private int page = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_list);

        LayoutInflater.from(this).inflate(R.layout.recycler_layout, (ViewGroup) findViewById(android.R.id.content), true);


        gson = new Gson();
        context = this;

        Intent intent = getIntent();
        ArrayList<Show> showResults = (ArrayList<Show>) intent.getSerializableExtra("shows");

        recyclerViewResults = findViewById(R.id.recyclerViewResults);
        adapter = new ShowsAdapter(this, showResults);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerViewResults.setLayoutManager(layoutManager);
        recyclerViewResults.setItemAnimator(new DefaultItemAnimator());
        recyclerViewResults.setAdapter(adapter);

    }

    private void getShowDetails(Show show){
        new GetShowDetailsTask(show).execute();
    }

    private class GetShowDetailsTask extends AsyncTask<Void, Void, Void> {

        private Show show;
        private Movie movie;
        private TVShow tvShow;
        private String showType;

        public GetShowDetailsTask(Show show){
            this.show = show;

            if(show instanceof Movie){
                showType = "movie";
            } else if(show instanceof TVShow){
                showType = "tv";
            }

            movie = null;
            tvShow = null;
        }

        @Override
        protected void onPreExecute(){
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Getting Show Details...");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Ion.with(context)
            .load("https://api.themoviedb.org/3/" + showType + "/" + show.getId() + "?api_key=" + api_key)
            .asJsonObject()
            .setCallback(new FutureCallback<JsonObject>() {
                @Override
                public void onCompleted(Exception e, JsonObject result) {
                    if(show instanceof Movie){
                        movie = (Movie) show;
                    } else if(show instanceof TVShow){
                        tvShow = (TVShow) show;
                    }

                    Genre[] genres = gson.fromJson(result.getAsJsonArray("genres"), Genre[].class);
                    String overview = result.get("overview").getAsString();
                    final String poster_path = result.get("backdrop_path").getAsString();
                    double vote_average = result.get("vote_average").getAsDouble();
                    int vote_count = result.get("vote_count").getAsInt();

                    String title = "";
                    String status = result.get("status").getAsString();
                    String release_date = "";

                    boolean adult = false;
                    int budget = 0;
                    int revenue = 0;
                    int runtime = 0;

                    if(movie != null){
                        adult = result.get("adult").getAsBoolean();
                        budget = result.get("budget").getAsInt();
                        revenue = result.get("revenue").getAsInt();
                        runtime = result.get("runtime").getAsInt();
                        release_date = result.get("release_date").getAsString();
                        title = result.get("title").getAsString();
                    } else if(tvShow != null){
                        release_date = result.get("first_air_date").getAsString();
                        title = result.get("name").getAsString();
                    }




                    if(movie != null){
                        movie.setGenres(genres);
                        movie.setOverview(overview);
                        movie.setPoster_path(poster_path);
                        movie.setVote_average(vote_average);
                        movie.setVote_count(vote_count);
                        movie.setAdult(adult);
                        movie.setBudget(budget);
                        movie.setRelease_date(release_date);
                        movie.setRevenue(revenue);
                        movie.setRuntime(runtime);
                        movie.setTitle(title);
                        movie.setStatus(status);
                    } else if(tvShow != null){
                        tvShow.setGenres(genres);
                        tvShow.setOverview(overview);
                        tvShow.setPoster_path(poster_path);
                        tvShow.setVote_average(vote_average);
                        tvShow.setVote_count(vote_count);
                        tvShow.setRelease_date(release_date);
                        tvShow.setTitle(title);
                        tvShow.setStatus(status);
                    }


                    if(movie != null){
                        loadDetailsFragment(movie);
                    } else if(tvShow != null){
                        loadDetailsFragment(tvShow);
                    }

                }
            });


            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            if(progressDialog != null && progressDialog.isShowing()){
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    private void loadDetailsFragment(Show show){
        Intent intent = new Intent(getApplicationContext(), PrimaryShowDetailsActivity.class);
        intent.putExtra("show", show);
        startActivity(intent);
    }
}
