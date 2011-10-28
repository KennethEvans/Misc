//Copyright (c) 2011 Kenneth Evans
//
//Permission is hereby granted, free of charge, to any person obtaining
//a copy of this software and associated documentation files (the
//"Software"), to deal in the Software without restriction, including
//without limitation the rights to use, copy, modify, merge, publish,
//distribute, sublicense, and/or sell copies of the Software, and to
//permit persons to whom the Software is furnished to do so, subject to
//the following conditions:
//
//The above copyright notice and this permission notice shall be included
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package net.kenevans.android.misc;

import android.net.Uri;

/**
 * Manages a ListView of all the messages in the database specified by the URI field.
 */
/**
 * @author evans
 * 
 */
/**
 * @author evans
 *
 */
public class SMSTest2Activity extends SMSActivity {
	/** The Uri to use. */
	public static final Uri uri = MMS_SMS_CONVERSATIONS_URI;
	/** The date multiplier to use to get ms.  MMS message timestamps are in sec not ms. */
	public static final Long dateMultiplier = 1L;

	/* (non-Javadoc)
	 * @see net.kenevans.android.misc.SMSActivity#getUri()
	 */
	@Override
	public Uri getUri() {
		return uri;
	}
	
	/* (non-Javadoc)
	 * @see net.kenevans.android.misc.SMSActivity#getDatemultiplier()
	 */
	@Override
	public Long getDateMultiplier() {
		return dateMultiplier;
	}

}
