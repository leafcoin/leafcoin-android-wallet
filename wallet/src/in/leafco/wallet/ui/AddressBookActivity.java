/*
 * Copyright 2011-2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package in.leafcoin.wallet.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.google.leafcoin.core.Address;
import com.google.leafcoin.core.ECKey;

import in.leafcoin.wallet.Constants;
import in.leafcoin.wallet.util.ViewPagerTabs;
import in.leafcoin.wallet.R;

/**
 * @author Andreas Schildbach
 */
public final class AddressBookActivity extends AbstractWalletActivity
{
	public static void start(final Context context, final boolean sending)
	{
		final Intent intent = new Intent(context, AddressBookActivity.class);
		intent.putExtra(EXTRA_SENDING, sending);
		context.startActivity(intent);
	}

	private static final String EXTRA_SENDING = "sending";

	private WalletAddressesFragment walletAddressesFragment;
	private SendingAddressesFragment sendingAddressesFragment;

    private static final String TAG_LEFT = "walletAddress";
    private static final String TAG_RIGHT = "sendingAddresses";

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.address_book_content);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

        FragmentManager fragmentManager = getSupportFragmentManager();

        walletAddressesFragment = (WalletAddressesFragment) fragmentManager.findFragmentByTag(TAG_LEFT);
        sendingAddressesFragment = (SendingAddressesFragment) fragmentManager.findFragmentByTag(TAG_RIGHT);

        FragmentTransaction remove = fragmentManager.beginTransaction();
        if (walletAddressesFragment == null) {
            walletAddressesFragment = new WalletAddressesFragment();
        } else {
            remove.remove(walletAddressesFragment);
        }
        if (sendingAddressesFragment == null) {
            sendingAddressesFragment = new SendingAddressesFragment();
        } else {
            remove.remove(sendingAddressesFragment);
        }
        if (!remove.isEmpty()) {
            remove.commit();
            fragmentManager.executePendingTransactions();
        }

        final ViewPager pager = (ViewPager) findViewById(R.id.address_book_pager);
        if (pager != null) {
            pager.setAdapter(new TwoFragmentAdapter(fragmentManager, walletAddressesFragment, sendingAddressesFragment));

            final ViewPagerTabs pagerTabs = (ViewPagerTabs) findViewById(R.id.address_book_pager_tabs);
            pagerTabs.addTabLabels(R.string.address_book_list_receiving_title, R.string.address_book_list_sending_title);

            pager.setOnPageChangeListener(pagerTabs);
            final int position = getIntent().getBooleanExtra(EXTRA_SENDING, true) ? 1 : 0;
            pager.setCurrentItem(position);
            pager.setPageMargin(2);
            pager.setPageMarginDrawable(R.color.bg_less_bright);

            pagerTabs.onPageSelected(position);
            pagerTabs.onPageScrolled(position, 0, 0);
        } else {
            fragmentManager.beginTransaction()
                    .add(R.id.wallet_addresses_fragment, walletAddressesFragment, TAG_LEFT)
                    .add(R.id.sending_addresses_fragment, sendingAddressesFragment, TAG_RIGHT)
                    .commit();
        }

		updateFragments();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				finish();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/* private */void updateFragments()
	{
		final List<ECKey> keys = getWalletApplication().getWallet().getKeys();
		final ArrayList<Address> addresses = new ArrayList<Address>(keys.size());

		for (final ECKey key : keys)
		{
			final Address address = key.toAddress(Constants.NETWORK_PARAMETERS);
			addresses.add(address);
		}

		sendingAddressesFragment.setWalletAddresses(addresses);
	}

    private static class TwoFragmentAdapter extends PagerAdapter {
        private final FragmentManager fragmentManager;
        private final Fragment left;
        private final Fragment right;
        private FragmentTransaction currentTransaction = null;
        private Fragment currentPrimaryItem = null;

        public TwoFragmentAdapter(FragmentManager fragmentManager, Fragment left, Fragment right) {
            this.fragmentManager = fragmentManager;
            this.left = left;
            this.right = right;
        }

        @Override public int getCount() {
            return 2;
        }

        @Override public Object instantiateItem(ViewGroup container, int position) {
            if (currentTransaction == null) {
                currentTransaction = fragmentManager.beginTransaction();
            }

            String tag = (position == 0) ? TAG_LEFT : TAG_RIGHT;
            Fragment fragment = (position == 0) ? left : right;
            currentTransaction.add(container.getId(), fragment, tag);
            if (fragment != currentPrimaryItem) {
                fragment.setMenuVisibility(false);
                fragment.setUserVisibleHint(false);
            }

            return fragment;
        }

        @Override public void destroyItem(ViewGroup container, int position, Object object) {
            // With right pages, fragments should never be destroyed.
            throw new AssertionError();
        }

        @Override public void setPrimaryItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (fragment != currentPrimaryItem) {
                if (currentPrimaryItem != null) {
                    currentPrimaryItem.setMenuVisibility(false);
                    currentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setMenuVisibility(true);
                    fragment.setUserVisibleHint(true);
                }
                currentPrimaryItem = fragment;
            }
        }

        @Override public void finishUpdate(ViewGroup container) {
            if (currentTransaction != null) {
                currentTransaction.commitAllowingStateLoss();
                currentTransaction = null;
                fragmentManager.executePendingTransactions();
            }
        }

        @Override public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }
    }
}
