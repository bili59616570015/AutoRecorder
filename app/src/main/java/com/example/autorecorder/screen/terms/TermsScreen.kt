package com.example.autorecorder.screen.terms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.autorecorder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onBackButtonClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.terms)) },
                modifier = Modifier.shadow(4.dp),
                navigationIcon = {
                    IconButton(onClick = onBackButtonClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding),
        ) {
            Text(
                text = """
                    1. 项目性质
                    本开源项目（以下简称"本项目"）仅供学习交流使用，并非商业产品。开发者自愿分享代码，但不对代码质量、安全性或适用性作任何明示或暗示的担保。
            
                    2. 责任限制
                    使用者无论以任何方式运用本项目代码（包括但不限于直接使用、修改、二次开发等），均应自行承担所有风险。因使用本项目导致的任何直接或间接损失（包括数据泄露、数据丢失、设备损坏、法律纠纷等），概不负责。
            
                    3. 知识产权
                    本项目中包含的第三方库/代码，其版权归属原作者。使用者应遵守各组件对应的开源协议。
            
                    4. 禁止条款
                    不得将本项目用于以下用途：
                    - 违反中华人民共和国法律法规的行为
                    - 破坏计算机信息系统安全
                    - 制作/传播恶意软件
                    - 其他有悖公序良俗的用途
            
                    5. 免责延伸
                    本声明适用于所有通过GitHub或其他渠道获取本项目代码的个体或组织。您一旦使用本项目，即视为已阅读并同意本声明全部内容。
                """.trimIndent(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        }
    }
}