package com.eaux.app.muzei.bgg;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.Preference;
import android.util.AttributeSet;

public class VersionPreference extends Preference {

	public VersionPreference(Context context) {
		super(context);
	}

	public VersionPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VersionPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	@Override
	public CharSequence getSummary() {
		try {
			PackageManager pm = getContext().getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(getContext().getPackageName(), 0);
			return pInfo.versionName;
		} catch (NameNotFoundException e) {
			return "?.?";
		}
	}
}
