package fr.insensa.photoepsi.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;


/**
 * Created by Jérôme on 24/04/2015.
 * Permet l'affichage de la boite de dialogue demandant à l'utilisateur
 * s'il veut envoyer la photo par mail.
 *
 */

    public class EmailDialog extends DialogFragment {

        public static EmailDialog newInstance(String path) {
            EmailDialog frag = new EmailDialog();
            Bundle args = new Bundle();
            args.putString("PATH", path);
            frag.setArguments(args);
            return frag;
        }

    public interface EmailDialogListener {
        public void doPositiveClick(DialogFragment dialogFragment, String path);
        public void doNegativeClick(DialogFragment dialogFragment, String path);
    }

    // Use this instance of the interface to deliver action events
    private EmailDialogListener mListener;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String path = getArguments().getString("PATH");
                return new AlertDialog.Builder(getActivity())
                        // .setIcon(R.drawable.alert_dialog_icon)
                        .setTitle("Voulez-vous l'envoyer par mail ?")
                        .setPositiveButton("oui",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mListener.doPositiveClick(EmailDialog.this, path);
                                    }
                                }
                        )
                        .setNegativeButton("non",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mListener.doNegativeClick(EmailDialog.this, path);
                                    }
                                }
                        )
                        .create();
        }


    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the EmailDialogListener so we can send events to the host
            mListener = (EmailDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement EmailDialogistener");
        }
    }
}
