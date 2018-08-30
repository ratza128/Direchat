package ro.upb.cs.direchat;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ro.upb.cs.direchat.ChatMessages.WiFiChatFragment;
import ro.upb.cs.direchat.Services.WiFiP2pServicesFragment;

import static android.media.CamcorderProfile.get;

/**
 * Clasa ce reprezinta un fragment ca TAB
 */
public class TabFragment extends android.support.v4.app.Fragment {

    private SectionsPagerAdapter mSectionAdapter;
    private ViewPager mViewPager;
    private static WiFiP2pServicesFragment wiFiP2pServicesFragment;
    private static List<WiFiChatFragment> wiFiChatFragmentList;

    public SectionsPagerAdapter getmSectionAdapter() {
        return mSectionAdapter;
    }

    public ViewPager getmViewPager() {
        return mViewPager;
    }

    public static WiFiP2pServicesFragment getWiFiP2pServicesFragment(){
        return wiFiP2pServicesFragment;
    }

    public static List<WiFiChatFragment> getWiFiChatFragmentList() { return wiFiChatFragmentList;}

    /**
     * Metoda ce intoarce instanta noului Fragment
     */
    public static TabFragment newInstance() {
        TabFragment fragment = new TabFragment();
        wiFiP2pServicesFragment = WiFiP2pServicesFragment.newInstance();
        wiFiChatFragmentList = new ArrayList<>();
        return fragment;
    }

    /**
     * constructorul default
     */
    public TabFragment() {}

    public WiFiChatFragment getChatFragmentByTab(int tabNumber) {
        return wiFiChatFragmentList.get(tabNumber - 1);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_tab, container, false);

        mSectionAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionAdapter);

        //Pun taburile la ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.tabs);
        tabs.setViewPager(mViewPager);

        //
        tabs.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mSectionAdapter.notifyDataSetChanged();
            }
        });
        return rootView;
    }

    /**
     * Metoda ce verifica daca numarul tabului este valid.
     *
     * Conditii pentru tabNum:
     * 1) 0 este rezervat pentru ServiceList
     * 2) mai mic sau egal decat 9
     *
     *
     * @param tabNum int Reprezinta numarul tabului pentru verificare
     * @return  true sau false, daca conditia este valida sau nu
     */
    public boolean isValidTabNum (int tabNum){
        return tabNum >= 1 && tabNum <= wiFiChatFragmentList.size();
    }

    /**
     * Clasa ce reprezinta FragmentPageAdapter a fragmentului selectat,
     * Returneaza fragmentul corespunzator tabului
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(android.support.v4.app.FragmentManager fm) {super(fm);}

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
                return wiFiP2pServicesFragment;
            else
                return wiFiChatFragmentList.get(position);
        }

        @Override
        public int getCount() {
            //deoarece primul fragment (nu in interiorul listei) este WiFiP2pServicesFragment
            return wiFiChatFragmentList.size() + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position){
                case 0:
                    return ("Services").toUpperCase(l);
                default:
                    return ("Chat" + position).toUpperCase(l);
            }
        }
    }
}
