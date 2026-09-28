#!/bin/bash
# Run from the project root (~/apk/3qar)
set -e
[ -f settings.gradle.kts ] || { echo "شغّل السكربت من مجلد المشروع ~/apk/3qar"; exit 1; }
J=app/src/main/java/com/delilaqar/realestate
R=app/src/main/res

# ---------- icon ----------
cat > $R/drawable/ic_check.xml <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z"/>
</vector>
EOF

# ---------- item layout ----------
cat > $R/layout/item_upgrade_request.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardBackgroundColor="@color/surface_card"
    app:cardCornerRadius="14dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="14dp">

        <TextView
            android:id="@+id/requestTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="@color/text_primary"
            android:textSize="15sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/requestMeta"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textColor="@color/text_secondary"
            android:textSize="12sp" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:orientation="horizontal">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/approveButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginEnd="8dp"
                android:layout_weight="1"
                android:backgroundTint="@color/accent_green"
                android:text="@string/admin_approve"
                android:textColor="@android:color/white"
                android:textSize="13sp"
                app:cornerRadius="24dp"
                app:icon="@drawable/ic_check"
                app:iconGravity="textStart"
                app:iconTint="@android:color/white" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/dismissButton"
                style="@style/OutlinedButtonDark"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/admin_dismiss" />
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
EOF

# ---------- fragment layout ----------
cat > $R/layout/fragment_admin.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_dark">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="14dp"
            android:text="@string/admin_title"
            android:textColor="@color/text_primary"
            android:textSize="18sp"
            android:textStyle="bold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/requestsRecyclerView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
    </LinearLayout>

    <TextView
        android:id="@+id/emptyStateText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="@string/admin_empty_state"
        android:textColor="@color/text_secondary"
        android:visibility="gone" />
</FrameLayout>
EOF

# ---------- adapter ----------
cat > $J/ui/AdminRequestAdapter.kt <<'EOF'
package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.delilaqar.realestate.databinding.ItemUpgradeRequestBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UpgradeRequest(
    val propertyId: String = "",
    val propertyTitle: String = "",
    val requestedAt: Long = 0
)

class AdminRequestAdapter(
    private val items: MutableList<UpgradeRequest>,
    private val onApprove: (UpgradeRequest) -> Unit,
    private val onDismiss: (UpgradeRequest) -> Unit
) : RecyclerView.Adapter<AdminRequestAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale("ar"))

    inner class ViewHolder(val binding: ItemUpgradeRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUpgradeRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = items[position]
        holder.binding.requestTitle.text = request.propertyTitle.ifEmpty { request.propertyId }
        holder.binding.requestMeta.text = "${dateFormat.format(Date(request.requestedAt))}  •  ${request.propertyId}"
        holder.binding.approveButton.setOnClickListener { onApprove(request) }
        holder.binding.dismissButton.setOnClickListener { onDismiss(request) }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<UpgradeRequest>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
EOF

# ---------- fragment ----------
cat > $J/ui/AdminFragment.kt <<'EOF'
package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.delilaqar.realestate.databinding.FragmentAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminFragment : Fragment() {
    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AdminRequestAdapter

    companion object {
        const val ADMIN_EMAIL = "apk.apk.mustafa@gmail.com"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser?.email != ADMIN_EMAIL) {
            findNavController().popBackStack()
            return
        }

        adapter = AdminRequestAdapter(mutableListOf(), ::approveRequest, ::dismissRequest)
        binding.requestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.requestsRecyclerView.adapter = adapter

        db.collection("upgradeRequests")
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null || snapshot == null) return@addSnapshotListener
                val requests = snapshot.documents.mapNotNull { doc ->
                    val propertyTitle = doc.getString("propertyTitle") ?: return@mapNotNull null
                    val requestedAt = doc.getLong("requestedAt") ?: 0
                    UpgradeRequest(propertyId = doc.id, propertyTitle = propertyTitle, requestedAt = requestedAt)
                }
                adapter.submitList(requests)
                binding.emptyStateText.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun approveRequest(request: UpgradeRequest) {
        val featuredUntil = System.currentTimeMillis() + UpgradeFeaturedFragment.DURATION_DAYS * 24L * 3600L * 1000L
        db.collection("properties").document(request.propertyId)
            .update("featuredUntil", featuredUntil)
            .addOnSuccessListener {
                db.collection("upgradeRequests").document(request.propertyId).delete()
                if (isAdded) Toast.makeText(requireContext(), "تم تفعيل الإعلان المميز", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "فشل التفعيل، تأكد من رقم الإعلان", Toast.LENGTH_SHORT).show()
            }
    }

    private fun dismissRequest(request: UpgradeRequest) {
        db.collection("upgradeRequests").document(request.propertyId).delete()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
EOF

# ---------- edits ----------
python3 - <<'PY'
def edit(path, old, new, count=1):
    s = open(path, encoding='utf-8').read()
    n = s.count(old)
    assert n == count, f"expected {count} match(es) in {path}, found {n}: {old[:60]!r}"
    open(path, 'w', encoding='utf-8').write(s.replace(old, new))

J = "app/src/main/java/com/delilaqar/realestate"
R = "app/src/main/res"

# 1) write the upgrade request before opening whatsapp
edit(f"{J}/ui/UpgradeFeaturedFragment.kt",
     "    private fun openWhatsapp() {\n",
     '''    private fun createUpgradeRequest() {
        if (propertyId.isEmpty()) return
        val request = hashMapOf(
            "propertyId" to propertyId,
            "propertyTitle" to propertyTitle,
            "requestedAt" to System.currentTimeMillis(),
            "status" to "pending"
        )
        FirebaseFirestore.getInstance().collection("upgradeRequests").document(propertyId).set(request)
    }

    private fun openWhatsapp() {
        createUpgradeRequest()
''')

# 2) strings
edit(f"{R}/values/strings.xml", "</resources>", '''    <string name="admin_title">طلبات الترقية لمميز</string>
    <string name="admin_empty_state">ماكو طلبات ترقية حالياً</string>
    <string name="admin_approve">تفعيل</string>
    <string name="admin_dismiss">تجاهل</string>
    <string name="admin_panel_button">لوحة التحكم</string>
</resources>''')

# 3) nav_graph: add adminFragment destination
edit(f"{R}/navigation/nav_graph.xml", "</navigation>", '''    <fragment
        android:id="@+id/adminFragment"
        android:name="com.delilaqar.realestate.ui.AdminFragment"
        android:label="لوحة التحكم"
        tools:layout="@layout/fragment_admin" />

</navigation>''')

# 4) profile layout: add admin button above logout button
edit(f"{R}/layout/fragment_profile.xml",
     '            <com.google.android.material.button.MaterialButton\n                android:id="@+id/logoutButton"',
     '''            <com.google.android.material.button.MaterialButton
                android:id="@+id/adminButton"
                style="@style/OutlinedButtonDark"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                android:text="@string/admin_panel_button"
                android:visibility="gone" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/logoutButton"''')

# 5) ProfileFragment.kt: show button only for admin + navigate
edit(f"{J}/ui/ProfileFragment.kt",
     "        val uid = auth.currentUser?.uid\n        if (uid == null) {\n",
     '''        if (auth.currentUser?.email == AdminFragment.ADMIN_EMAIL) {
            binding.adminButton.visibility = View.VISIBLE
            binding.adminButton.setOnClickListener { findNavController().navigateSafe(R.id.adminFragment, Bundle()) }
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
''')
print("تم تطبيق كل التعديلات")
PY
