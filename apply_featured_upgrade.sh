#!/bin/bash
# Run from the project root (~/apk/3qar)
set -e
[ -f settings.gradle.kts ] || { echo "شغّل السكربت من مجلد المشروع ~/apk/3qar"; exit 1; }
J=app/src/main/java/com/delilaqar/realestate
R=app/src/main/res

# ---------- QR image ----------
echo "iVBORw0KGgoAAAANSUhEUgAAAaQAAAGkAQAAAABWN2yYAAAEeUlEQVR42u2cO7KrRhCG/0aqAkeQOUQ78BJgCd6Bl8KwA2/BO3DoUJQjh94BCm+GMlQX8TuYhwakg6Rzy9HpiZA0H6pquvo9CPH+Oif4zFJKKaVeWuQxXOczIOSEagIApASAcgZ28S4OH/zXtPo8q+SVcoqxjz4O99sSACntAoDyKvITjzsaoPkmzheU03KXSl6pbepfkaQAAJwSIME/GWqgK5Z7/xaRXyPqT+AWrVQTRqAia4AkOUfm7S+VvFLvUt0eAFoRA8iBIslL/zWvTKhKXqnPU+WM6rvU1u8O4CkhyV5lqNQbuUOIxnLyKLcsgcFAVctdd7nDIbo24Ur1UKl3qDj17D/cOa2oq9gFUH67SjOhGlGNToHZC/9I4l0qeaU27WG0Qi2FNCmdZSu5XoPKUKkfoIbH1EUkQ1tAZMeeUu/YZv73fHZ19S5DZ01boZJXapPqrPqcxX93lRqos4X9k3r9X97yOXto48NqWIWII6oRzaCSV+qphx1RkQZATvbCI1CNPod1TpekTzo2uhUqeaUcVa9zh/2Lma/JycG2yIQkzaLCcgvyoHqo1HOqEzFnkcIpWVuvdp1ERMQAbRFRvugi1h46HWyGZbfiCN/QVckrte1hsfM5bDmjGlHZb23MZs2a2Xl7d7NsC6v50ITWe5W8Ui9mHDkNgLNIIex2bG3XLPNmzNrDZkSXvWfZepX8F6T2AC6CFO3vwDWZQQEARnc77acD0GUjLoL8LuPgIM6dVhOQEmlU5kNKjmg2M44JiHscj2Zg9HkptVgGQBGoi5CU8ioHAMI2d37ZlV8OwiPQ5QTAXmWo1NMcdkQzgIO47q3r1trcobf2Lr2P9MI6IEovVPJfnvIdVjN8uG1eOUI/gWx9JzmhpG+chT6srfQ1I5A+9rAJDribQO5XqqnP60tRZmtXsFzIQqTnq3VRpOdry344PuTDWltW6nktZUJDmtxathmlN3ZOz0KBzg6deI2alu708FyhVfJq2baoPeqLADCFnYS/Sg3AFZIFJ3/neqG9tuyX002GVmE+yg703eLDZ5mvPmWlXMZhrDuF7ZohGhGwkd5R/LzooF0zpV6gbsZweLhnkRgU8D2OlAYhzfUHy5x2lsEvez+teqjUpmUL1zkPkFvf1XcrxJ2tyJA0N40KvhPs3RRBKOjl8y2HHcPpH5X8l6eyrYxjMYEMl6ouvgqjKg2J8j3LVgdDq89LqQ3KDRbb2WII28SNkLZZ9FqPLlMZKvWih91FZ804u6EUXxKOLFs4bjbYzPfBOqrklfqBHBZATuRcvhylXL3iKKXJae48rN3lk18bFpJxCDnCqIdV6mVK2O3I79KMqM4iGWBfLFDb+O8s/KanF5V6g6L8Mkcx2kXYwxdKriIZ2p8fTtwVjz8n92dvVfJKRY5wRdWxKtbRnaSAiIjsfWR3ERT+uuLSKuKC6ggA/tSazM79nguq5JV6Ql3CyX9bvbtK7TNbf6CRIhlwEauan5nuU8l/RermKFcJ6iHWjQIQfW+qUkr9b9R//mAkO5qdzfwAAAAASUVORK5CYII=" | base64 -d > $R/drawable/qr_zaincash_payment.png

# ---------- icons ----------
cat > $R/drawable/ic_star.xml <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/accent_orange"
        android:pathData="M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z"/>
</vector>
EOF

cat > $R/drawable/bg_star_circle.xml <<'EOF'
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#33FF9500" />
</shape>
EOF

# ---------- layout ----------
cat > $R/layout/fragment_upgrade_featured.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_dark">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/upgrade_featured_title"
            android:textColor="@color/text_primary"
            android:textSize="20sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/upgradePropertyTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textColor="@color/text_secondary"
            android:textSize="13sp" />

        <TextView
            android:id="@+id/upgradePriceText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:textColor="@color/accent_orange"
            android:textSize="18sp"
            android:textStyle="bold" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:lineSpacingExtra="4dp"
            android:text="@string/upgrade_featured_instructions"
            android:textColor="@color/text_secondary"
            android:textSize="13sp" />

        <ImageView
            android:id="@+id/upgradeQrImage"
            android:layout_width="220dp"
            android:layout_height="220dp"
            android:layout_gravity="center_horizontal"
            android:layout_marginTop="20dp"
            android:background="@android:color/white"
            android:padding="8dp"
            android:src="@drawable/qr_zaincash_payment"
            android:contentDescription="@string/upgrade_qr_desc" />

        <TextView
            android:id="@+id/upgradeWalletText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:layout_marginTop="12dp"
            android:textColor="@color/text_primary"
            android:textIsSelectable="true"
            android:textSize="15sp"
            android:textStyle="bold" />

        <Button
            android:id="@+id/upgradeWhatsappButton"
            style="@style/WhatsappButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="@string/upgrade_featured_confirm_whatsapp" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
EOF

# ---------- fragment ----------
cat > $J/ui/UpgradeFeaturedFragment.kt <<'EOF'
package com.delilaqar.realestate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentUpgradeFeaturedBinding
import com.delilaqar.realestate.util.CurrencyFormatter
import com.google.firebase.firestore.FirebaseFirestore

class UpgradeFeaturedFragment : Fragment() {
    private var _binding: FragmentUpgradeFeaturedBinding? = null
    private val binding get() = _binding!!

    private var propertyId: String = ""
    private var propertyTitle: String = ""

    companion object {
        const val PRICE_IQD = 2000.0
        const val DURATION_DAYS = 3
        const val WALLET_NUMBER = "07858055717"
        const val WHATSAPP_NUMBER = "+9647824553729"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpgradeFeaturedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        propertyId = arguments?.getString("propertyId").orEmpty()

        binding.upgradePriceText.text = getString(
            R.string.upgrade_featured_price_format,
            CurrencyFormatter.format(PRICE_IQD),
            DURATION_DAYS
        )
        binding.upgradeWalletText.text = getString(R.string.upgrade_featured_wallet_number, WALLET_NUMBER)

        if (propertyId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("properties").document(propertyId).get()
                .addOnSuccessListener { doc ->
                    if (_binding == null) return@addOnSuccessListener
                    propertyTitle = doc.getString("title").orEmpty()
                    binding.upgradePropertyTitle.text = propertyTitle
                }
        }

        binding.upgradeWhatsappButton.setOnClickListener { openWhatsapp() }
    }

    private fun openWhatsapp() {
        val message = "مرحباً، حوّلت ${CurrencyFormatter.format(PRICE_IQD)} لترقية إعلاني إلى مميز " +
            "لمدة $DURATION_DAYS أيام.\nالإعلان: $propertyTitle\nرقم الإعلان: $propertyId\n(مرفق صورة إشعار التحويل)"
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$WHATSAPP_NUMBER&text=${Uri.encode(message)}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            if (isAdded) Toast.makeText(requireContext(), "تطبيق واتساب غير مثبت على جهازك", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
EOF

# ---------- edits to existing files ----------
python3 - <<'PY'
def edit(path, old, new):
    s = open(path, encoding='utf-8').read()
    assert s.count(old) == 1, f"pattern not found exactly once in {path}: {old[:50]!r}"
    open(path, 'w', encoding='utf-8').write(s.replace(old, new))

J = "app/src/main/java/com/delilaqar/realestate"
R = "app/src/main/res"

edit(f"{J}/data/Property.kt",
     "    var featured: Boolean = false,\n",
     "    var featured: Boolean = false,\n    var featuredUntil: Long = 0,\n")

edit(f"{J}/ui/HomeFragment.kt",
     '.whereEqualTo("featured", true)',
     '.whereGreaterThan("featuredUntil", System.currentTimeMillis())')

edit(f"{R}/values/strings.xml", "</resources>", """    <string name="upgrade_featured_title">ترقية الإعلان إلى مميز</string>
    <string name="upgrade_featured_price_format">السعر: %1$s لمدة %2$d أيام</string>
    <string name="upgrade_featured_instructions">حوّل المبلغ بمسح كود QR أدناه من تطبيق زين كاش، أو حوّل يدوياً لرقم المحفظة الظاهر تحت الكود. بعد التحويل اضغط زر واتساب وأرسل صورة إشعار التحويل، وسيتم تفعيل الإعلان المميز بعد التأكد من وصول المبلغ.</string>
    <string name="upgrade_featured_wallet_number">رقم المحفظة: %1$s</string>
    <string name="upgrade_featured_confirm_whatsapp">تأكيد التحويل عبر واتساب</string>
    <string name="upgrade_qr_desc">كود QR لمحفظة زين كاش</string>
    <string name="upgrade_button_desc">ترقية إلى مميز</string>
</resources>""")

edit(f"{R}/layout/item_my_listing.xml",
     '        <ImageView\n            android:id="@+id/editButton"',
     '''        <ImageView
            android:id="@+id/upgradeButton"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginEnd="8dp"
            android:padding="10dp"
            android:background="@drawable/bg_star_circle"
            android:src="@drawable/ic_star"
            android:contentDescription="@string/upgrade_button_desc" />

        <ImageView
            android:id="@+id/editButton"''')

edit(f"{J}/ui/MyListingsAdapter.kt",
     "    private val onEditClick: (Property) -> Unit\n",
     "    private val onEditClick: (Property) -> Unit,\n    private val onUpgradeClick: (Property) -> Unit\n")
edit(f"{J}/ui/MyListingsAdapter.kt",
     "        binding.editButton.setOnClickListener { onEditClick(property) }\n",
     "        binding.editButton.setOnClickListener { onEditClick(property) }\n        binding.upgradeButton.setOnClickListener { onUpgradeClick(property) }\n")

edit(f"{J}/ui/ProfileFragment.kt",
     "                findNavController().navigateSafe(R.id.postAdFragment, bundle)\n            }\n        )",
     "                findNavController().navigateSafe(R.id.postAdFragment, bundle)\n            },\n            onUpgradeClick = { property ->\n                val bundle = Bundle().apply { putString(\"propertyId\", property.id) }\n                findNavController().navigateSafe(R.id.upgradeFeaturedFragment, bundle)\n            }\n        )")

edit(f"{R}/navigation/nav_graph.xml", "</navigation>", """    <fragment
        android:id="@+id/upgradeFeaturedFragment"
        android:name="com.delilaqar.realestate.ui.UpgradeFeaturedFragment"
        android:label="ترقية لمميز"
        tools:layout="@layout/fragment_upgrade_featured">
        <argument
            android:name="propertyId"
            app:argType="string" />
    </fragment>

</navigation>""")
print("تم تطبيق كل التعديلات")
PY
