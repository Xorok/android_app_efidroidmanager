package org.efidroid.efidroidmanager.fragments.operatingsystemedit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.OSEditFragmentInteractionListener;

import java.io.InputStream;
import java.util.ArrayList;

public class GeneralFragment extends Fragment {
    // data
    private OperatingSystem mOperatingSystem = null;

    // listener
    private OSEditFragmentInteractionListener mListener;

    // request codes
    private static final int RESULT_IMAGE_PICKER = 1;

    private EditText mEditTextName;
    private EditText mEditTextDescription;
    private ImageView mIcon = null;

    public GeneralFragment() {
        // Required empty public constructor
    }

    public static GeneralFragment newInstance() {
        return new GeneralFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OSEditFragmentInteractionListener) {
            mListener = (OSEditFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must be OSEditFragmentInteractionListener");
        }

        mOperatingSystem = mListener.getOperatingSystem();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mOperatingSystem = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_general, container, false);

        // get views
        View iconEntry = view.findViewById(R.id.icon_entry);
        AppCompatSpinner mLocationSpinner = (AppCompatSpinner) view.findViewById(R.id.spinner_location);
        AppCompatSpinner mOSTypeSpinner = (AppCompatSpinner) view.findViewById(R.id.spinner_ostype);
        mIcon = (ImageView) view.findViewById(R.id.image);
        mEditTextName = (EditText) view.findViewById(R.id.name);
        mEditTextDescription = (EditText) view.findViewById(R.id.description);

        // location spinner
        final ArrayList<OperatingSystemEditActivity.MultibootDir> multibootDirectories = mListener.getMultibootDirectories();
        mLocationSpinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, multibootDirectories));
        mLocationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                OperatingSystemEditActivity.MultibootDir multibootDir = multibootDirectories.get(position);
                mOperatingSystem.setLocation(multibootDir);
                mOperatingSystem.notifyChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // ostype spinner
        mOSTypeSpinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, OperatingSystem.getLocalizedOSTypeList(getContext())));
        mOSTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mOperatingSystem.setOperatingSystemType(OperatingSystem.ALL_OS_TYPES.get(position));
                mOperatingSystem.notifyChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // icon entry
        iconEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, null), RESULT_IMAGE_PICKER);
            }
        });
        iconEntry.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mOperatingSystem.setIconUri(null);
                mOperatingSystem.notifyChange();
                loadImage();
                return true;
            }
        });

        // name
        mEditTextName.setText(mOperatingSystem.getName());
        mEditTextName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mOperatingSystem.setName(mEditTextName.getText().toString());
            }
        });

        // description
        mEditTextDescription.setText(mOperatingSystem.getDescription());
        mEditTextDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mOperatingSystem.setDescription(mEditTextDescription.getText().toString());
            }
        });

        // image
        loadImage();

        return view;
    }

    private void loadImage() {
        Drawable drawable;
        Uri imageUri = mOperatingSystem.getIconUri();

        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            drawable = Drawable.createFromStream(inputStream, imageUri.toString());
        } catch (Exception e) {
            drawable = ResourcesCompat.getDrawable(getResources(), android.R.mipmap.sym_def_app_icon, getActivity().getTheme());
        }

        mIcon.setImageDrawable(drawable);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_IMAGE_PICKER:
                if (resultCode== Activity.RESULT_OK) {
                    Uri imageUri = data.getData();
                    mOperatingSystem.setIconUri(imageUri);
                    mOperatingSystem.notifyChange();
                    loadImage();
                }
                break;
        }
    }
}