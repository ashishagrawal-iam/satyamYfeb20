package com.wiom.csp.ui.policies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Policies screen: simple scrollable text screen displaying
 * Terms of Service, Privacy Policy, and SLA Terms sections.
 * Back button at top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoliciesScreen(
    hindi: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (hindi) "नीतियाँ" else "Policies",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Terms of Service ────────────────────────────────────
            PolicySection(
                title = if (hindi) "सेवा की शर्तें" else "Terms of Service",
                content = if (hindi) TERMS_OF_SERVICE_HI else TERMS_OF_SERVICE_EN
            )

            SectionDivider()

            // ── Privacy Policy ──────────────────────────────────────
            PolicySection(
                title = if (hindi) "गोपनीयता नीति" else "Privacy Policy",
                content = if (hindi) PRIVACY_POLICY_HI else PRIVACY_POLICY_EN
            )

            SectionDivider()

            // ── SLA Terms ───────────────────────────────────────────
            PolicySection(
                title = if (hindi) "SLA शर्तें" else "SLA Terms",
                content = if (hindi) SLA_TERMS_HI else SLA_TERMS_EN
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 24.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// ── Policy Content ──────────────────────────────────────────────────────

private const val TERMS_OF_SERVICE_EN = """
By accessing and using the Wiom CSP Partner application, you agree to be bound by these Terms of Service. As a Channel Service Partner (CSP), you are authorized to manage NetBox installations, service tasks, and customer interactions on behalf of Wiom.

1. Partner Obligations
You agree to perform all assigned tasks within the Service Level Agreement (SLA) timelines, maintain the security of your account credentials, and ensure accurate reporting of all field activities including installations, restores, and NetBox pickups.

2. Equipment Responsibility
NetBox units assigned to you remain the property of Wiom. You are responsible for their safekeeping and timely return. Security deposits are held against assigned units and are subject to the carry fee schedule disclosed in the Deposit section.

3. Revenue and Settlements
Assurance revenue is calculated based on your active base count and settled on the published cycle. Wiom reserves the right to adjust settlements for SLA violations, equipment losses, or fraudulent activity.

4. Account Termination
Either party may terminate this partnership with 30 days written notice. Upon termination, all assigned NetBox units must be returned and pending settlements will be processed within 45 business days.

5. Modifications
Wiom reserves the right to modify these terms at any time. Continued use of the application after modifications constitutes acceptance of the updated terms.
"""

private const val TERMS_OF_SERVICE_HI = """
Wiom CSP पार्टनर एप्लिकेशन तक पहुँचने और उपयोग करने से, आप इन सेवा शर्तों से बाध्य होने के लिए सहमत हैं। एक चैनल सेवा पार्टनर (CSP) के रूप में, आप Wiom की ओर से नेटबॉक्स इंस्टॉलेशन, सेवा कार्य और ग्राहक इंटरैक्शन का प्रबंधन करने के लिए अधिकृत हैं।

1. पार्टनर दायित्व
आप सर्विस लेवल एग्रीमेंट (SLA) समयसीमा के भीतर सभी सौंपे गए कार्यों को पूरा करने, अपने खाता क्रेडेंशियल्स की सुरक्षा बनाए रखने, और इंस्टॉलेशन, रिस्टोर और नेटबॉक्स पिकअप सहित सभी फील्ड गतिविधियों की सटीक रिपोर्टिंग सुनिश्चित करने के लिए सहमत हैं।

2. उपकरण जिम्मेदारी
आपको सौंपी गई नेटबॉक्स इकाइयाँ Wiom की संपत्ति हैं। आप उनकी सुरक्षित रखवाली और समय पर वापसी के लिए जिम्मेदार हैं। सुरक्षा जमा राशि सौंपी गई इकाइयों के विरुद्ध रखी जाती है और जमा अनुभाग में प्रकट कैरी शुल्क अनुसूची के अधीन है।

3. राजस्व और निपटान
एश्योरेंस राजस्व आपके सक्रिय बेस गणना के आधार पर गणना की जाती है और प्रकाशित चक्र पर निपटान किया जाता है। Wiom SLA उल्लंघन, उपकरण हानि, या धोखाधड़ी गतिविधि के लिए निपटान समायोजित करने का अधिकार सुरक्षित रखता है।

4. खाता समाप्ति
कोई भी पक्ष 30 दिन की लिखित सूचना के साथ इस साझेदारी को समाप्त कर सकता है। समाप्ति पर, सभी सौंपी गई नेटबॉक्स इकाइयाँ वापस की जानी चाहिए और लंबित निपटान 45 कार्य दिवसों के भीतर संसाधित किए जाएंगे।

5. संशोधन
Wiom किसी भी समय इन शर्तों को संशोधित करने का अधिकार सुरक्षित रखता है। संशोधनों के बाद एप्लिकेशन का निरंतर उपयोग अपडेट की गई शर्तों की स्वीकृति माना जाएगा।
"""

private const val PRIVACY_POLICY_EN = """
Wiom is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your personal information when you use the CSP Partner application.

1. Information We Collect
We collect your name, phone number, location data (for task assignments), device information, and activity logs related to task performance and NetBox management.

2. How We Use Your Information
Your information is used to authenticate your identity, assign and track service tasks, calculate settlements and assurance revenue, monitor SLA compliance, and communicate service-related updates.

3. Data Sharing
We do not sell your personal information. We may share limited data with Wiom operations teams for task coordination, payment processors for settlement disbursements, and law enforcement when required by applicable Indian law.

4. Data Security
We employ industry-standard encryption for data in transit and at rest. Access to partner data is restricted to authorized Wiom personnel on a need-to-know basis.

5. Data Retention
Your data is retained for the duration of your partnership and for a period of 5 years thereafter for regulatory compliance. You may request data deletion by contacting support.

6. Your Rights
Under applicable data protection laws, you have the right to access, correct, or delete your personal data. Contact our support team to exercise these rights.
"""

private const val PRIVACY_POLICY_HI = """
Wiom आपकी गोपनीयता की रक्षा के लिए प्रतिबद्ध है। यह गोपनीयता नीति बताती है कि जब आप CSP पार्टनर एप्लिकेशन का उपयोग करते हैं तो हम आपकी व्यक्तिगत जानकारी कैसे एकत्र, उपयोग और सुरक्षित करते हैं।

1. हम जो जानकारी एकत्र करते हैं
हम आपका नाम, फ़ोन नंबर, स्थान डेटा (कार्य असाइनमेंट के लिए), डिवाइस जानकारी, और कार्य प्रदर्शन और नेटबॉक्स प्रबंधन से संबंधित गतिविधि लॉग एकत्र करते हैं।

2. हम आपकी जानकारी का उपयोग कैसे करते हैं
आपकी जानकारी का उपयोग आपकी पहचान प्रमाणित करने, सेवा कार्य असाइन और ट्रैक करने, निपटान और एश्योरेंस राजस्व की गणना करने, SLA अनुपालन की निगरानी करने और सेवा-संबंधित अपडेट संप्रेषित करने के लिए किया जाता है।

3. डेटा साझाकरण
हम आपकी व्यक्तिगत जानकारी नहीं बेचते। हम कार्य समन्वय के लिए Wiom संचालन टीमों, निपटान वितरण के लिए भुगतान प्रोसेसर, और लागू भारतीय कानून द्वारा आवश्यक होने पर कानून प्रवर्तन के साथ सीमित डेटा साझा कर सकते हैं।

4. डेटा सुरक्षा
हम ट्रांज़िट और रेस्ट में डेटा के लिए उद्योग-मानक एन्क्रिप्शन का उपयोग करते हैं। पार्टनर डेटा तक पहुँच अधिकृत Wiom कर्मियों तक सीमित है।

5. डेटा प्रतिधारण
आपका डेटा आपकी साझेदारी की अवधि और उसके बाद नियामक अनुपालन के लिए 5 वर्ष की अवधि के लिए रखा जाता है। आप सहायता से संपर्क करके डेटा हटाने का अनुरोध कर सकते हैं।

6. आपके अधिकार
लागू डेटा संरक्षण कानूनों के तहत, आपको अपने व्यक्तिगत डेटा तक पहुँचने, सही करने या हटाने का अधिकार है। इन अधिकारों का प्रयोग करने के लिए हमारी सहायता टीम से संपर्क करें।
"""

private const val SLA_TERMS_EN = """
The Service Level Agreement (SLA) defines the performance standards expected of all CSP Partners operating within the Wiom network.

1. Performance Domains
Your SLA is evaluated across multiple domains including installation quality, restoration timeliness, NetBox return compliance, and customer satisfaction metrics.

2. Metric Thresholds
Each domain has defined metric thresholds. Falling below the standard threshold triggers a WARNING standing. Falling below the severe threshold triggers a BREACH standing. Metrics are evaluated on a rolling window basis with a minimum sample requirement.

3. Consequences of Breach
SLA breaches may result in reduced task routing (fewer new task offers), suspension of bonus eligibility, and in severe cases, partnership review.

4. Hysteresis Recovery
Recovering from a breach requires maintaining clean performance windows. The number of consecutive clean windows required is specified per domain. This prevents oscillation between standings.

5. Bonus Eligibility
Partners maintaining COMPLIANT standing across all domains are eligible for performance bonuses calculated on their assurance revenue base.

6. Evaluation Cycle
SLA standings are re-evaluated on a regular cycle. The next evaluation date is visible in the SLA section of the application.

7. Disputes
If you believe an SLA evaluation is incorrect, you may raise a support case within 7 days of the evaluation. The review team will assess and respond within 5 business days.
"""

private const val SLA_TERMS_HI = """
सर्विस लेवल एग्रीमेंट (SLA) Wiom नेटवर्क के भीतर संचालित सभी CSP पार्टनरों से अपेक्षित प्रदर्शन मानकों को परिभाषित करता है।

1. प्रदर्शन डोमेन
आपके SLA का मूल्यांकन इंस्टॉलेशन गुणवत्ता, रिस्टोरेशन समयबद्धता, नेटबॉक्स रिटर्न अनुपालन, और ग्राहक संतुष्टि मेट्रिक्स सहित कई डोमेन में किया जाता है।

2. मेट्रिक थ्रेशोल्ड
प्रत्येक डोमेन में परिभाषित मेट्रिक थ्रेशोल्ड हैं। मानक थ्रेशोल्ड से नीचे गिरने पर WARNING स्थिति ट्रिगर होती है। गंभीर थ्रेशोल्ड से नीचे गिरने पर BREACH स्थिति ट्रिगर होती है। मेट्रिक्स का मूल्यांकन न्यूनतम नमूना आवश्यकता के साथ रोलिंग विंडो आधार पर किया जाता है।

3. उल्लंघन के परिणाम
SLA उल्लंघन के परिणामस्वरूप कम कार्य रूटिंग (कम नए कार्य ऑफर), बोनस पात्रता का निलंबन, और गंभीर मामलों में साझेदारी समीक्षा हो सकती है।

4. हिस्टेरेसिस रिकवरी
उल्लंघन से उबरने के लिए लगातार स्वच्छ प्रदर्शन विंडो बनाए रखना आवश्यक है। आवश्यक लगातार स्वच्छ विंडो की संख्या प्रति डोमेन निर्दिष्ट है।

5. बोनस पात्रता
सभी डोमेन में COMPLIANT स्थिति बनाए रखने वाले पार्टनर अपने एश्योरेंस राजस्व आधार पर गणना किए गए प्रदर्शन बोनस के लिए पात्र हैं।

6. मूल्यांकन चक्र
SLA स्थिति का नियमित चक्र पर पुनर्मूल्यांकन किया जाता है। अगली मूल्यांकन तिथि एप्लिकेशन के SLA अनुभाग में दिखाई देती है।

7. विवाद
यदि आपको लगता है कि SLA मूल्यांकन गलत है, तो आप मूल्यांकन के 7 दिनों के भीतर एक सहायता केस उठा सकते हैं। समीक्षा टीम 5 कार्य दिवसों के भीतर मूल्यांकन और प्रतिक्रिया देगी।
"""
