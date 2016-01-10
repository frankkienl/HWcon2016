package nl.frankkie.hwcon2016;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

/**
 * Created by FrankkieNL on 1-1-2016.
 */
public class NewsListAdapter extends CursorAdapter {

    public NewsListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.news_listview_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;

    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        String imageStr = cursor.getString(NewsFragment.COL_IMAGE);
        String title = cursor.getString(NewsFragment.COL_TITLE);
        String url = cursor.getString(NewsFragment.COL_URL);
        viewHolder.mTitle.setText(title);
        Ion.with(context).load(imageStr)
                .withBitmap()
                .error(R.mipmap.ic_launcher)
                .placeholder(R.mipmap.ic_launcher)
                .intoImageView(viewHolder.mImage);
    }

    public class ViewHolder {
        public final ImageView mImage;
        public final TextView mTitle;

        public ViewHolder(View view) {
            this.mImage = (ImageView) view.findViewById(R.id.news_image);
            this.mTitle = (TextView) view.findViewById(R.id.news_title);
        }
    }
}
