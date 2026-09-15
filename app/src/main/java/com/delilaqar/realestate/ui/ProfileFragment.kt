package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentProfileBinding
import com.delilaqar.realestate.util.GoogleAuthHelper
import com.delilaqar.realestate.util.NsfwModelManager
import com.delilaqar.realestate.util.navigateSafe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var myListingsAdapter: MyListingsAdapter

    private val statusHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var statusRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            showLoggedOut()
        } else {
            showLoggedIn(uid)
        }
    }

    private fun showLoggedOut() {
        binding.loggedOutSection.visibility = View.VISIBLE
        binding.loggedInSection.visibility = View.GONE
        binding.goToLoginButton.setOnClickListener {
            findNavController().navigateSafe(R.id.loginFragment)
        }
    }

    private fun showLoggedIn(uid: String) {
        binding.loggedOutSection.visibility = View.GONE
        binding.loggedInSection.visibility = View.VISIBLE

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val name = doc.getString("name") ?: "مستخدم"
                val email = doc.getString("email") ?: ""
                val phone = doc.getString("phone") ?: ""

                binding.profileName.text = name
                binding.profileEmail.text = email
                binding.profilePhone.text = phone.ifEmpty { "لا يوجد رقم هاتف - اضغط للإضافة" }
                binding.avatarText.text = name.firstOrNull()?.uppercase() ?: "?"

                binding.profilePhone.setOnClickListener {
                    GoogleAuthHelper.promptForPhoneNumber(requireContext(), uid) {
                        showLoggedIn(uid)
                    }
                }
            }

        binding.logoutButton.setOnClickListener {
            GoogleAuthHelper.getSignInClient(requireContext()).signOut()
            auth.signOut()
            showLoggedOut()
        }

        myListingsAdapter = MyListingsAdapter(
            items = emptyList(),
            onDeleteClick = { property -> confirmDeleteListing(property) }
        )
        binding.myListingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myListingsRecyclerView.adapter = myListingsAdapter

        loadMyListings(uid)
        setupModelStatus()
    }

    private fun setupModelStatus() {
        binding.modelRetryButton.setOnClickListener {
            NsfwModelManager.retry(requireContext())
            refreshModelStatus()
        }
        refreshModelStatus()
    }

    private fun refreshModelStatus() {
        if (_binding == null) return

        var nextDelay = 3000L

        when (NsfwModelManager.state) {
            NsfwModelManager.State.READY -> {
                binding.modelStatusCard.visibility = View.GONE
                return
            }
            NsfwModelManager.State.DOWNLOADING -> {
                binding.modelStatusCard.visibility = View.VISIBLE
                binding.modelProgressBar.visibility = View.VISIBLE
                binding.modelStatusText.text = "⏳ جاري تجهيز نظام فحص الصور لأول مرة"
                binding.modelPercentText.text = "${NsfwModelManager.progressPercent}%"
                binding.modelProgressBar.progress = NsfwModelManager.progressPercent
                binding.modelRetryButton.visibility = View.GONE
                nextDelay = 800
            }
            NsfwModelManager.State.FAILED -> {
                binding.modelStatusCard.visibility = View.VISIBLE
                binding.modelProgressBar.visibility = View.GONE
                binding.modelPercentText.text = ""
                binding.modelStatusText.text = "⚠️ تعذّر تجهيز نظام فحص الصور بسبب مشكلة اتصال. تأكد من النت وأعد المحاولة"
                binding.modelRetryButton.visibility = View.VISIBLE
            }
            NsfwModelManager.State.IDLE -> {
                binding.modelStatusCard.visibility = View.VISIBLE
                binding.modelProgressBar.visibility = View.VISIBLE
                binding.modelPercentText.text = "0%"
                binding.modelStatusText.text = "⏳ جاري تجهيز نظام فحص الصور..."
                binding.modelRetryButton.visibility = View.GONE
                NsfwModelManager.startBackgroundDownload(requireContext())
                nextDelay = 800
            }
        }

        statusRunnable?.let { statusHandler.removeCallbacks(it) }
        statusRunnable = Runnable { refreshModelStatus() }
        statusHandler.postDelayed(statusRunnable!!, nextDelay)
    }

    private fun loadMyListings(uid: String) {
        db.collection("properties")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                val listings = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Property::class.java)?.apply { id = doc.id }
                }.sortedByDescending { it.createdAt }

                myListingsAdapter.updateData(listings)
                binding.myListingsCountText.text = "${listings.size} إعلان"
                binding.myListingsEmptyText.visibility = if (listings.isEmpty()) View.VISIBLE else View.GONE
                binding.myListingsRecyclerView.visibility = if (listings.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                if (isAdded) Toast.makeText(requireContext(), "فشل تحميل إعلاناتك: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDeleteListing(property: Property) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("حذف الإعلان")
            .setMessage("هل أنت متأكد من رغبتك في حذف إعلان \"${property.title}\"؟ لا يمكن التراجع عن هذا الإجراء.")
            .setPositiveButton("حذف") { _, _ -> deleteListing(property) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun deleteListing(property: Property) {
        db.collection("properties").document(property.id).delete()
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "تم حذف الإعلان", Toast.LENGTH_SHORT).show()
                    auth.currentUser?.uid?.let { loadMyListings(it) }
                }
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "فشل حذف الإعلان: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        statusRunnable?.let { statusHandler.removeCallbacks(it) }
        _binding = null
    }
}
