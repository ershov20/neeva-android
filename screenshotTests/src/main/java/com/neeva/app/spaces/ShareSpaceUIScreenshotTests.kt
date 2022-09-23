package com.neeva.app.spaces

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.spaces.share.ShareSpaceSheetPreview_NonPublicSpace_InviteUser_Dark
import com.neeva.app.spaces.share.ShareSpaceSheetPreview_NonPublicSpace_Invite_UserLight
import com.neeva.app.spaces.share.ShareSpaceSheetPreview_PublicSpace_NoInviteUser_Dark
import com.neeva.app.spaces.share.ShareSpaceSheetPreview_PublicSpace_NoInviteUser_Light
import org.junit.Test

class ShareSpaceUIScreenshotTests : BaseScreenshotTest() {
    @Test
    fun shareSpaceSheetPreview_NonPublicSpace_Invite_UserLight() = runScreenshotTest {
        ShareSpaceSheetPreview_NonPublicSpace_Invite_UserLight()
    }

    @Test
    fun shareSpaceSheetPreview_NonPublicSpace_InviteUser_Dark() = runScreenshotTest {
        ShareSpaceSheetPreview_NonPublicSpace_InviteUser_Dark()
    }

    @Test
    fun shareSpaceSheetPreview_PublicSpace_NoInviteUser_Light() = runScreenshotTest {
        ShareSpaceSheetPreview_PublicSpace_NoInviteUser_Light()
    }

    @Test
    fun shareSpaceSheetPreview_PublicSpace_NoInviteUser_Dark() = runScreenshotTest {
        ShareSpaceSheetPreview_PublicSpace_NoInviteUser_Dark()
    }
}
