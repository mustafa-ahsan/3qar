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
import com.delilaqar.realestate.util.navigateSafe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var myListingsAdapter: MyListingsAdapter

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
                binding.profilePhone.text = phone.ifEmpty { "لا يوجد رقم هاتف" }
                binding.avatarText.text = name.firstOrNull()?.uppercase() ?: "?"
            }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            showLoggedOut()
        }

        binding.seedButton.setOnClickListener { seedSampleData(uid) }

        myListingsAdapter = MyListingsAdapter(
            items = emptyList(),
            onDeleteClick = { property -> confirmDeleteListing(property) }
        )
        binding.myListingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myListingsRecyclerView.adapter = myListingsAdapter

        loadMyListings(uid)
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
            .setMessage("متأكد تبي تحذف إعلان \"${property.title}\"؟ لا يمكن التراجع عن هذا الإجراء.")
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

    private fun seedSampleData(ownerId: String) {
        val samples = listOf(
            hashMapOf(
                "title" to "شقة حديثة غرفتين وصالة للإيجار في الكرادة",
                "price" to 6000,
                "listingType" to "rent",
                "propertyType" to "apartment",
                "cityId" to "baghdad",
                "district" to "الكرادة، بغداد",
                "bedrooms" to 2,
                "bathrooms" to 2,
                "area" to 120,
                "featured" to true,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar2/800/600"),
                "ownerId" to ownerId
            ),
            hashMapOf(
                "title" to "فيلا فاخرة أربع غرف مع حديقة للبيع في حي الجزائر",
                "price" to 180000,
                "listingType" to "sale",
                "propertyType" to "villa",
                "cityId" to "basra",
                "district" to "حي الجزائر، البصرة",
                "bedrooms" to 4,
                "bathrooms" to 4,
                "area" to 350,
                "featured" to false,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar3/800/600"),
                "ownerId" to ownerId
            ),
            hashMapOf(
                "title" to "شقة مفروشة بالكامل ثلاث غرف للإيجار في عنكاوا",
                "price" to 8000,
                "listingType" to "rent",
                "propertyType" to "apartment",
                "cityId" to "erbil",
                "district" to "عنكاوا، أربيل",
                "bedrooms" to 3,
                "bathrooms" to 2,
                "area" to 160,
                "featured" to false,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar4/800/600"),
                "ownerId" to ownerId
            ),
            hashMapOf(
                "title" to "قطعة أرض سكنية قريبة من حرم الإمام علي للبيع",
                "price" to 95000,
                "listingType" to "sale",
                "propertyType" to "land",
                "cityId" to "najaf",
                "district" to "قرب الحرم، النجف",
                "bedrooms" to 0,
                "bathrooms" to 0,
                "area" to 300,
                "featured" to false,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar5/800/600"),
                "ownerId" to ownerId
            ),
            hashMapOf(
                "title" to "شقة غرفتين وصالة للإيجار في حي الأندلس بالناصرية",
                "price" to 4000,
                "listingType" to "rent",
                "propertyType" to "apartment",
                "cityId" to "nasiriyah",
                "district" to "حي الأندلس، الناصرية",
                "bedrooms" to 2,
                "bathrooms" to 1,
                "area" to 100,
                "featured" to true,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar6/800/600"),
                "ownerId" to ownerId
            )
        )

        var completed = 0
        samples.forEach { data ->
            db.collection("properties").add(data)
                .addOnSuccessListener {
                    completed++
                    if (completed == samples.size && isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "✅ تمت إضافة ${samples.size} عقارات تجريبية",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.currentUser?.uid?.let { loadMyListings(it) }
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), "فشلت إضافة عقار: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
