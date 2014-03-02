package com.eaux.app.muzei.bgg;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

public class ViewUriPreference extends Preference {

	private String mUri;

	public ViewUriPreference(Context context) {
		super(context);
		init();
	}

	public ViewUriPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		parseAttributeSet(context, attrs);
		init();
	}

	public ViewUriPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		parseAttributeSet(context, attrs);
		init();
	}

	@Override
	public CharSequence getSummary() {
		CharSequence summary = super.getSummary();
		if (TextUtils.isEmpty(summary)) {
			if (!TextUtils.isEmpty(mUri)) {
				return Uri.parse(mUri).getHost();
			}
		}
		return summary;
	}

	private void parseAttributeSet(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewUriPreference, 0, 0);
		mUri = a.getString(R.styleable.ViewUriPreference_uri);
		a.recycle();
	}

	private void init() {
		if (!TextUtils.isEmpty(mUri)) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mUri));
			intent.addCategory(Intent.CATEGORY_BROWSABLE);
			setIntent(intent);
		}
	}
}
